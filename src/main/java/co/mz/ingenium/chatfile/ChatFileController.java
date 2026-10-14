package co.mz.ingenium.chatfile;

import co.mz.ingenium.entity.UserEntity.User;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatFileController {

    private final ChatFileService chatFileService;

    public ChatFileController(ChatFileService chatFileService) {
        this.chatFileService = chatFileService;
    }

    /**
     * Envia um anexo (imagem, áudio ou ficheiro) para a conversa com otherUserId.
     * Corpo: multipart/form-data, campo "file". Para áudio, "durationSeconds" é opcional.
     */
    @PostMapping(path = "/{otherUserId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatFileResponse upload(@AuthenticationPrincipal User user,
                                   @PathVariable UUID otherUserId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {
        return chatFileService.upload(user.getId(), otherUserId, file, durationSeconds);
    }
}
