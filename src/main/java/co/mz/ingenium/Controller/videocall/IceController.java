package co.mz.ingenium.Controller.videocall;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


//Entrega ao browser a lista de servidores STUN/TURN.credenciais do TURN ficam no servidor (application.properties) e não no JavaScript.

@RestController
public class IceController {

    @Value("${videocall.ice.stun:stun:stun.l.google.com:19302}")
    private List<String> stunUrls;

    @Value("${videocall.ice.turn.urls:}")
    private List<String> turnUrls;

    @Value("${videocall.ice.turn.username:}")
    private String turnUsername;

    @Value("${videocall.ice.turn.credential:}")
    private String turnCredential;

    @GetMapping("/api/call/ice")
    public Map<String, Object> iceServers() {
        List<Map<String, Object>> servers = new ArrayList<>();

        if (!stunUrls.isEmpty()) {
            Map<String, Object> stun = new LinkedHashMap<>();
            stun.put("urls", stunUrls);
            servers.add(stun);
        }
        List<String> turn = turnUrls.stream().filter(u -> !u.isBlank()).toList();
        if (!turn.isEmpty()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("urls", turn);
            t.put("username", turnUsername);
            t.put("credential", turnCredential);
            servers.add(t);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("iceServers", servers);
        return body;
    }
}
