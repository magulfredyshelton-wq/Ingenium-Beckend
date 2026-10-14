package co.mz.ingenium.call;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Entradas STOMP do toque. O cliente envia para /app/call.invite, /app/call.accept,
 * /app/call.reject ou /app/call.cancel, e recebe os avisos em /user/queue/calls.
 */
@Controller
public class CallSignalController {

    private final CallInviteService calls;

    public CallSignalController(CallInviteService calls) {
        this.calls = calls;
    }

    @MessageMapping("/call.invite")
    public void invite(@Payload @Valid CallInviteRequest dto, Principal principal) {
        calls.invite(UUID.fromString(principal.getName()), dto.roomId(), dto.video());
    }

    @MessageMapping("/call.accept")
    public void accept(@Payload @Valid CallActionRequest dto, Principal principal) {
        calls.accept(UUID.fromString(principal.getName()), dto.roomId());
    }

    @MessageMapping("/call.reject")
    public void reject(@Payload @Valid CallActionRequest dto, Principal principal) {
        calls.reject(UUID.fromString(principal.getName()), dto.roomId());
    }

    @MessageMapping("/call.cancel")
    public void cancel(@Payload @Valid CallActionRequest dto, Principal principal) {
        calls.cancel(UUID.fromString(principal.getName()), dto.roomId());
    }
}
