package co.mz.ingenium.call;

import co.mz.ingenium.config.videocall.CallAccessService;
import co.mz.ingenium.entity.UserEntity.User;
import co.mz.ingenium.repository.UserRepository.UserRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Convites de chamada a tocar. Só guarda o que está A TOCAR: depois de aceite, recusado,
 * cancelado ou perdido, o convite desaparece (a chamada em si passa para a sala /call).
 * O estado está em memória: se o servidor reiniciar, os convites a tocar perdem-se.
 */
@Service
public class CallInviteService {

    private static final Logger log = LoggerFactory.getLogger(CallInviteService.class);

    /** Quanto tempo toca antes de ficar "perdida". */
    private static final long RING_SECONDS = 45;

    private record Invite(UUID roomId, UUID callerId, UUID calleeId, boolean video) {
    }

    // Tudo isto é protegido por "synchronized" nos métodos desta classe.
    private final Map<UUID, Invite> ringing = new HashMap<>();
    private final Map<UUID, ScheduledFuture<?>> timers = new HashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "call-ring-timeout");
        t.setDaemon(true);
        return t;
    });

    private final CallAccessService access;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;
    private final CallLogService callLog;

    public CallInviteService(CallAccessService access, UserRepository users,
                             SimpMessagingTemplate messaging, CallLogService callLog) {
        this.access = access;
        this.users = users;
        this.messaging = messaging;
        this.callLog = callLog;
    }

    /** Quem liga: avisa o outro e confirma a quem liga que está a tocar. */
    public synchronized void invite(UUID callerId, UUID roomId, boolean video) {
        Optional<UUID> callee = access.otherParticipant(roomId, callerId);
        if (callee.isEmpty()) {
            sendTo(callerId, CallEvent.error(roomId, "forbidden"));
            return;
        }
        UUID calleeId = callee.get();

        // Já está a tocar nesta conversa, ou um dos dois está noutro convite a tocar.
        if (ringing.containsKey(roomId) || isRinging(callerId) || isRinging(calleeId)
                || callLog.isInCall(callerId) || callLog.isInCall(calleeId)) {
            sendTo(callerId, CallEvent.busy(roomId));
            return;
        }

        ringing.put(roomId, new Invite(roomId, callerId, calleeId, video));
        timers.put(roomId, scheduler.schedule(() -> timeout(roomId), RING_SECONDS, TimeUnit.SECONDS));

        sendTo(calleeId, CallEvent.of("incoming", roomId, callerId, displayName(callerId), video));
        sendTo(callerId, CallEvent.of("ringing", roomId, calleeId, displayName(calleeId), video));
    }

    /** Quem recebe aceita: os dois passam a entrar na sala /call. */
    public synchronized void accept(UUID calleeId, UUID roomId) {
        Invite invite = ringing.get(roomId);
        if (invite == null) {
            sendTo(calleeId, CallEvent.error(roomId, "no-invite"));
            return;
        }
        if (!invite.calleeId().equals(calleeId)) {
            sendTo(calleeId, CallEvent.error(roomId, "forbidden"));
            return;
        }
        remove(roomId);
        callLog.onAccepted(roomId, invite.callerId(), calleeId, invite.video());
        sendTo(invite.callerId(), CallEvent.of("accepted", roomId, calleeId, displayName(calleeId), invite.video()));
        sendTo(calleeId, CallEvent.of("accepted", roomId, invite.callerId(), displayName(invite.callerId()), invite.video()));
    }

    /** Quem recebe recusa. */
    public synchronized void reject(UUID calleeId, UUID roomId) {
        Invite invite = ringing.get(roomId);
        if (invite == null) {
            sendTo(calleeId, CallEvent.error(roomId, "no-invite"));
            return;
        }
        if (!invite.calleeId().equals(calleeId)) {
            sendTo(calleeId, CallEvent.error(roomId, "forbidden"));
            return;
        }
        remove(roomId);
        sendTo(invite.callerId(), CallEvent.of("rejected", roomId, calleeId, displayName(calleeId), invite.video()));
        callLog.recordRejected(roomId, invite.callerId(), calleeId, invite.video());
    }

    /** Quem ligou desiste antes de atenderem. */
    public synchronized void cancel(UUID callerId, UUID roomId) {
        Invite invite = ringing.get(roomId);
        if (invite == null) {
            sendTo(callerId, CallEvent.error(roomId, "no-invite"));
            return;
        }
        if (!invite.callerId().equals(callerId)) {
            sendTo(callerId, CallEvent.error(roomId, "forbidden"));
            return;
        }
        remove(roomId);
        sendTo(invite.calleeId(), CallEvent.of("cancelled", roomId, callerId, displayName(callerId), invite.video()));
        callLog.recordMissed(roomId, callerId, invite.calleeId(), invite.video());
    }

    /** Ninguém atendeu a tempo. */
    private synchronized void timeout(UUID roomId) {
        Invite invite = ringing.get(roomId);
        if (invite == null) {
            return;
        }
        remove(roomId);
        sendTo(invite.calleeId(), CallEvent.of("missed", roomId, invite.callerId(), displayName(invite.callerId()), invite.video()));
        sendTo(invite.callerId(), CallEvent.of("missed", roomId, invite.calleeId(), displayName(invite.calleeId()), invite.video()));
        callLog.recordMissed(roomId, invite.callerId(), invite.calleeId(), invite.video());
    }

    /** Quem ligava fechou a aplicação ou perdeu a ligação: o convite que fez deixa de tocar. */
    @EventListener
    public synchronized void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }
        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            return;
        }
        List<Invite> toCancel = new ArrayList<>();
        for (Invite invite : ringing.values()) {
            if (invite.callerId().equals(userId)) {
                toCancel.add(invite);
            }
        }
        for (Invite invite : toCancel) {
            remove(invite.roomId());
            sendTo(invite.calleeId(), CallEvent.of("cancelled", invite.roomId(), userId, displayName(userId), invite.video()));
            callLog.recordMissed(invite.roomId(), userId, invite.calleeId(), invite.video());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    // ---------- auxiliares ----------

    private boolean isRinging(UUID userId) {
        for (Invite invite : ringing.values()) {
            if (invite.callerId().equals(userId) || invite.calleeId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private void remove(UUID roomId) {
        ringing.remove(roomId);
        ScheduledFuture<?> timer = timers.remove(roomId);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    private String displayName(UUID userId) {
        return users.findById(userId).map(User::getDisplayName).orElse("Utilizador");
    }

    private void sendTo(UUID userId, CallEvent event) {
        try {
            messaging.convertAndSendToUser(userId.toString(), "/queue/calls", event);
        } catch (Exception e) {
            log.warn("Não foi possível avisar {}: {}", userId, e.getMessage());
        }
    }
}
