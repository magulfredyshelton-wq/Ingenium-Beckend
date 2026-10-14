package co.mz.ingenium.entity.UserEntity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "usuario")
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type")
@Getter
@Setter
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String email;
    private String password;

    /** @username público: único, minúsculas, 3 a 30 caracteres (letras, números, _ e .). */
    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    /** Foto de perfil: id do registo na tabela file (null = sem foto). */
    @Column(name = "avatar_file_id")
    private UUID avatarFileId;

    public abstract String getDisplayName();
}