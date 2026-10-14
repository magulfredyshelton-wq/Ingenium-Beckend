package co.mz.ingenium.ai.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Tag da base de dados (tabela tag). Só leitura: as tags são geridas pelo backend. */
@Entity
@Immutable
@Table(name = "tag")
public class Tag {

    @Id
    private UUID id;

    private String name;
    private String slug;
    private boolean active;

    // Tag-mãe (tabela tag_category)
    @Column(name = "tag_category_id")
    private UUID categoriaId;

    // Nome da tag-mãe, calculado pela própria consulta SQL
    @Formula("(select c.name from tag_category c where c.id = tag_category_id)")
    private String categoriaNome;

    protected Tag() {
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public boolean isActive() { return active; }
    public UUID getCategoriaId() { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}