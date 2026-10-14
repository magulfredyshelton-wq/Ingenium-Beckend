package co.mz.ingenium.dto.sharing;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class ShareDtos {

    private ShareDtos() {}

    public record Person(
            UUID id,
            String name,
            String username,
            String avatarUrl) {}

    public record PeoplePage(
            List<Person> items,
            boolean hasMore) {}

    public record Request(
            @NotEmpty @Size(max = 20)
            List<@NotNull UUID> recipientIds) {}

    public record Result(int sent) {}
}