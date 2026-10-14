package co.mz.ingenium.Controller.videocall;

import co.mz.ingenium.call.CallLogService;
import co.mz.ingenium.config.videocall.CallAccessService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CallHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CallHandler.class);
    private static final Pattern ROOM_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final ObjectMapper mapper = new ObjectMapper();
    private final int maxParticipants;
    private final CallAccessService access;
    private final CallLogService callLog;

    /** roomId -> (peerId -> Peer). Acesso protegido por synchronized(this). */
    private final Map<String, Map<String, Peer>> rooms = new HashMap<>();
    /** sessionId -> Peer (só existe depois do "join"). */
    private final Map<String, Peer> peersBySession = new ConcurrentHashMap<>();
    /** sessionId -> sessão protegida para envio concorrente. */
    private final Map<String, WebSocketSession> safeSessions = new ConcurrentHashMap<>();

    private static final class Peer {
        final String id;
        final WebSocketSession session;
        String room;
        String name;
        String userId;
        volatile boolean audio = true;
        volatile boolean video = true;
        volatile boolean screen = false;

        Peer(String id, WebSocketSession session) {
            this.id = id;
            this.session = session;
        }
    }

    public CallHandler(int maxParticipants, CallAccessService access, CallLogService callLog) {
        this.maxParticipants = maxParticipants;
        this.access = access;
        this.callLog = callLog;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // sendMessage() não é thread-safe; o decorator serializa os envios e protege contra clientes lentos
        safeSessions.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, 10_000, 512 * 1024));
        log.info("Ligação aberta: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession rawSession, TextMessage message) {
        WebSocketSession session = safeSessions.getOrDefault(rawSession.getId(), rawSession);

        JsonNode msg;
        try {
            msg = mapper.readTree(message.getPayload());
        } catch (Exception e) {
            return; // JSON inválido: ignora
        }

        switch (msg.path("type").asText("")) {
            case "join"   -> join(session, msg);
            case "signal" -> relaySignal(session, msg);
            case "state"  -> updateState(session, msg);
            case "leave"  -> leave(session);
            case "ping"   -> send(session, mapper.createObjectNode().put("type", "pong"));
            default       -> { /* ignora */ }
        }
    }


    private void join(WebSocketSession session, JsonNode msg) {
        if (peersBySession.containsKey(session.getId())) return;
        String room = msg.path("room").asText("");
        if (!ROOM_PATTERN.matcher(room).matches()) {
            sendError(session, "invalid-room");
            return;
        }

        // Quem é: vem do token (guardado no handshake), nunca do que o cliente escreve.
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            sendError(session, "unauthorized");
            return;
        }
        // A sala é a conversa (ChatRoom): só os dois participantes entram.
        if (!access.canJoin(UUID.fromString(userId), room)) {
            sendError(session, "forbidden");
            return;
        }

        Peer peer = new Peer(session.getId(), session);
        peer.room = room;
        peer.userId = userId;
        String tokenName = (String) session.getAttributes().get("name");
        peer.name = sanitizeName(tokenName == null ? "" : tokenName);
        peer.audio = msg.path("audio").asBoolean(true);
        peer.video = msg.path("video").asBoolean(true);

        ObjectNode welcome = mapper.createObjectNode();
        List<Peer> others;

        synchronized (this) {
            Map<String, Peer> members = rooms.computeIfAbsent(room, r -> new LinkedHashMap<>());
            if (members.size() >= maxParticipants) {
                sendError(session, "room-full");
                return;
            }
            welcome.put("type", "welcome").put("id", peer.id).put("max", maxParticipants);
            ArrayNode list = welcome.putArray("peers");
            for (Peer p : members.values()) list.add(describe(p));

            others = new ArrayList<>(members.values());
            members.put(peer.id, peer);
            peersBySession.put(session.getId(), peer);
        }

        callLog.peerJoined(UUID.fromString(room), UUID.fromString(userId));
        send(session, welcome);

        ObjectNode joined = describe(peer);
        joined.put("type", "peer-joined");
        for (Peer o : others) send(o.session, joined);

        log.info("'{}' entrou na sala {} ({} participantes)", peer.name, room, others.size() + 1);
    }

    private void leave(WebSocketSession session) {
        Peer peer = peersBySession.remove(session.getId());
        if (peer == null) return;
        if (peer.userId != null) {
            callLog.peerLeft(UUID.fromString(peer.room), UUID.fromString(peer.userId));
        }

        List<Peer> others;
        synchronized (this) {
            Map<String, Peer> members = rooms.get(peer.room);
            if (members == null) return;
            members.remove(peer.id);
            others = new ArrayList<>(members.values());
            if (members.isEmpty()) rooms.remove(peer.room);
        }

        ObjectNode left = mapper.createObjectNode().put("type", "peer-left").put("id", peer.id);
        for (Peer o : others) send(o.session, left);

        log.info("'{}' saiu da sala {}", peer.name, peer.room);
    }


    private void relaySignal(WebSocketSession session, JsonNode msg) {
        Peer from = peersBySession.get(session.getId());
        if (from == null) return;

        Peer target = findInRoom(from.room, msg.path("to").asText(""));
        if (target == null) return;

        ObjectNode out = mapper.createObjectNode();
        out.put("type", "signal").put("from", from.id);
        out.set("data", msg.path("data"));
        send(target.session, out);
    }

    private void updateState(WebSocketSession session, JsonNode msg) {
        Peer peer = peersBySession.get(session.getId());
        if (peer == null) return;

        peer.audio = msg.path("audio").asBoolean(peer.audio);
        peer.video = msg.path("video").asBoolean(peer.video);
        peer.screen = msg.path("screen").asBoolean(false);

        ObjectNode out = mapper.createObjectNode();
        out.put("type", "state").put("from", peer.id).put("audio", peer.audio)
                .put("video", peer.video).put("screen", peer.screen);
        for (Peer o : othersInRoom(peer)) send(o.session, out);
    }



    private synchronized Peer findInRoom(String room, String peerId) {
        Map<String, Peer> members = rooms.get(room);
        return members == null ? null : members.get(peerId);
    }

    private synchronized List<Peer> othersInRoom(Peer self) {
        Map<String, Peer> members = rooms.get(self.room);
        List<Peer> list = new ArrayList<>();
        if (members != null) {
            for (Peer p : members.values()) if (!p.id.equals(self.id)) list.add(p);
        }
        return list;
    }

    private ObjectNode describe(Peer p) {
        ObjectNode n = mapper.createObjectNode();
        n.put("id", p.id).put("userId", p.userId).put("name", p.name).put("audio", p.audio).put("video", p.video).put("screen", p.screen);
        return n;
    }

    private static String sanitizeName(String raw) {
        String s = raw.replaceAll("\\p{Cntrl}", "").trim();
        if (s.length() > 40) s = s.substring(0, 40);
        return s.isEmpty() ? "Participante" : s;
    }

    private void sendError(WebSocketSession session, String code) {
        send(session, mapper.createObjectNode().put("type", "error").put("code", code));
    }

    private void send(WebSocketSession session, JsonNode payload) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar para {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSession safe = safeSessions.getOrDefault(session.getId(), session);
        leave(safe);
        safeSessions.remove(session.getId());
        log.info("Ligação fechada: {} ({})", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Erro de transporte em {}: {}", session.getId(), exception.getMessage());
    }
}
