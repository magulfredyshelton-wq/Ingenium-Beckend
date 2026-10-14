package co.mz.ingenium.call;

import java.util.UUID;

/**
 * Aviso enviado para /user/queue/calls.
 * type: incoming | ringing | accepted | rejected | cancelled | missed | busy | error
 * from/fromName: a outra pessoa da chamada (em "incoming" é quem está a ligar).
 * reason: só em "error" (forbidden, no-invite).
 */
public record CallEvent(String type, UUID roomId, UUID from, String fromName, boolean video, String reason) {

    static CallEvent of(String type, UUID roomId, UUID from, String fromName, boolean video) {
        return new CallEvent(type, roomId, from, fromName, video, null);
    }

    static CallEvent error(UUID roomId, String reason) {
        return new CallEvent("error", roomId, null, null, false, reason);
    }

    static CallEvent busy(UUID roomId) {
        return new CallEvent("busy", roomId, null, null, false, null);
    }
}
