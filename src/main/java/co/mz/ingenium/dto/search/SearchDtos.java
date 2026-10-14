package co.mz.ingenium.dto.search;

import co.mz.ingenium.dto.Authentication.UserType;

import java.util.List;
import java.util.UUID;

public final class SearchDtos {

    private SearchDtos() {}

    public record Account(
            UUID id,
            String username,
            String name,
            UserType type,
            String avatarUrl) {}

    public record AccountsPage(
            List<Account> items,
            int page,
            int size,
            boolean hasMore) {}
}