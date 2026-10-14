package co.mz.ingenium.call;

import co.mz.ingenium.dto.Chat.ResponseMessageDTO;
import co.mz.ingenium.service.ChatService.MessagingService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Regista as chamadas no histórico do chat e sabe quem está numa chamada.
 * Uma chamada aceite fica "ativa"; conta como ligada quando os dois estão na sala /call,
 * e acaba (com a duração) quando um deles sai. Estado em memória.
 */
@Service
public class CallLogService {

    private static final Logger log = LoggerFactory.getLogger(CallLogService.class);

    /** Se depois de aceite os dois não chegarem a ligar-se neste tempo, a chamada é descartada. */
    private static final long CONNECT_TIMEOUT_SECONDS = 60;

    private static final class ActiveCall {
        final UUID roomId;
        final UUID callerId;
        final UUID calleeId;
        final boolean video;
        final Map<UUID, Integer> present = new HashMap<>(); // utilizador -> nº de sessões na sala
        Instant connectedAt;
        ScheduledFuture<?> connectTimer;

        ActiveCall(UUID roomId, UUID callerId, UUID calleeId, boolean video) {
            this.roomId = roomId;
            this.callerId = callerId;
            this.calleeId = calleeId;
            this.video = video;
        }
    }

    private final Object lock = new Object();
    private final Map<UUID, ActiveCall> active = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "call-connect-timeout");
        t.setDaemon(true);
        return t;
    });

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messaging;

    public CallLogService(MessagingService messagingService, SimpMessagingTemplate messaging) {
        this.messagingService = messagingService;
        this.messaging = messaging;
    }

    /** O utilizador está numa chamada aceite (ainda a ligar ou já ligada)? */
    public boolean isInCall(UUID userId) {
        synchronized (lock) {
            for (ActiveCall call : active.values()) {
                if (call.callerId.equals(userId) || call.calleeId.equals(userId)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Chamado quando quem recebeu aceita. */
    public void onAccepted(UUID roomId, UUID callerId, UUID calleeId, boolean video) {
        synchronized (lock) {
            ActiveCall call = new ActiveCall(roomId, callerId, calleeId, video);
            call.connectTimer = scheduler.schedule(() -> dropIfNotConnected(roomId),
                    CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            active.put(roomId, call);
        }
    }

    /** Chamado pelo /call quando alguém entra na sala. */
    public void peerJoined(UUID roomId, UUID userId) {
        synchronized (lock) {
            ActiveCall call = active.get(roomId);
            if (call == null) {
                return; // entrou sem convite: não registamos
            }
            call.present.merge(userId, 1, Integer::sum);
            if (call.connectedAt == null
                    && call.present.containsKey(call.callerId)
                    && call.present.containsKey(call.calleeId)) {
                call.connectedAt = Instant.now();
                if (call.connectTimer != null) {
                    call.connectTimer.cancel(false);
                }
            }
        }
    }

    /** Chamado pelo /call quando alguém sai da sala (ou perde a ligação). */
    public void peerLeft(UUID roomId, UUID userId) {
        ActiveCall ended = null;
        int seconds = 0;

        synchronized (lock) {
            ActiveCall call = active.get(roomId);
            if (call == null) {
                return;
            }
            call.present.computeIfPresent(userId, (k, v) -> v > 1 ? v - 1 : null);

            if (call.connectedAt != null && !call.present.containsKey(userId)) {
                // Estavam ligados e um saiu de vez: a chamada terminou.
                seconds = (int) Math.max(0, Duration.between(call.connectedAt, Instant.now()).getSeconds());
                active.remove(roomId);
                ended = call;
            } else if (call.connectedAt == null && call.present.isEmpty()) {
                // Aceite mas nunca chegaram a ligar-se: descarta sem registo.
                if (call.connectTimer != null) {
                    call.connectTimer.cancel(false);
                }
                active.remove(roomId);
            }
        }

        if (ended != null) {
            record(ended.roomId, ended.callerId, ended.calleeId,
                    CallRecordCodec.encode("ENDED", ended.video, seconds));
        }
    }

    /** Ninguém atendeu, ou quem ligou desistiu. */
    public void recordMissed(UUID roomId, UUID callerId, UUID calleeId, boolean video) {
        record(roomId, callerId, calleeId, CallRecordCodec.encode("MISSED", video, 0));
    }

    /** Quem recebeu recusou. */
    public void recordRejected(UUID roomId, UUID callerId, UUID calleeId, boolean video) {
        record(roomId, callerId, calleeId, CallRecordCodec.encode("REJECTED", video, 0));
    }

    private void dropIfNotConnected(UUID roomId) {
        synchronized (lock) {
            ActiveCall call = active.get(roomId);
            if (call != null && call.connectedAt == null) {
                active.remove(roomId);
            }
        }
    }

    /** Guarda a mensagem CALL (remetente = quem ligou) e entrega-a aos dois em tempo real. */
    private void record(UUID roomId, UUID callerId, UUID calleeId, String payload) {
        try {
            ResponseMessageDTO dto = messagingService.recordCall(roomId, callerId, payload);
            messaging.convertAndSendToUser(callerId.toString(), "/queue/messages", dto);
            messaging.convertAndSendToUser(calleeId.toString(), "/queue/messages", dto);
        } catch (Exception e) {
            log.warn("Não foi possível registar a chamada da sala {}: {}", roomId, e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
