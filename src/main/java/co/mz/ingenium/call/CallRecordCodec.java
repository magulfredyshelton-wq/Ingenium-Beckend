package co.mz.ingenium.call;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato da mensagem de chamada no histórico (type = CALL). O campo "message" guarda:
 *   {"status":"ENDED","video":true,"seconds":201}
 * status: ENDED (atendida e terminada), MISSED (ninguém atendeu / cancelada), REJECTED (recusada).
 * O remetente da mensagem é sempre quem ligou.
 */
public final class CallRecordCodec {

    private static final Pattern STATUS = Pattern.compile("\"status\"\\s*:\\s*\"([A-Z]+)\"");
    private static final Pattern VIDEO = Pattern.compile("\"video\"\\s*:\\s*(true|false)");
    private static final Pattern SECONDS = Pattern.compile("\"seconds\"\\s*:\\s*(\\d+)");

    private CallRecordCodec() {
    }

    public static String encode(String status, boolean video, int seconds) {
        return String.format(Locale.ROOT, "{\"status\":\"%s\",\"video\":%b,\"seconds\":%d}", status, video, seconds);
    }

    /** Texto curto para a lista de conversas. */
    public static String preview(String json) {
        if (json == null) {
            return "Chamada";
        }
        String status = find(STATUS, json, "");
        boolean video = "true".equals(find(VIDEO, json, "false"));
        int seconds = 0;
        try {
            seconds = Integer.parseInt(find(SECONDS, json, "0"));
        } catch (NumberFormatException ignored) {
            // fica 0
        }
        String kind = video ? "Chamada de vídeo" : "Chamada de voz";
        return switch (status) {
            case "ENDED" -> kind + " · " + String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
            case "MISSED" -> "Chamada perdida";
            case "REJECTED" -> "Chamada recusada";
            default -> "Chamada";
        };
    }

    private static String find(Pattern pattern, String text, String fallback) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : fallback;
    }
}
