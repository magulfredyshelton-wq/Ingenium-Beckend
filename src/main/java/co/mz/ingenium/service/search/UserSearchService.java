package co.mz.ingenium.service.search;

import co.mz.ingenium.dto.Authentication.UserType;
import co.mz.ingenium.dto.Feed.FeedItemDTO;
import co.mz.ingenium.dto.Feed.FeedPageDTO;
import co.mz.ingenium.dto.search.SearchDtos;
import co.mz.ingenium.repository.Feed.FeedItemRepository;
import co.mz.ingenium.storage.StorageService;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserSearchService {

    private final NamedParameterJdbcTemplate jdbc;
    private final FeedItemRepository feedItems;
    private final StorageService storage;

    public UserSearchService(
            NamedParameterJdbcTemplate jdbc,
            FeedItemRepository feedItems,
            StorageService storage) {
        this.jdbc = jdbc;
        this.feedItems = feedItems;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public SearchDtos.AccountsPage accounts(
            String username,
            int page,
            int size) {
        String term = normalize(username, false);
        int safePage = safePage(page);
        int safeSize = safeSize(size);

        if (term.isEmpty()) {
            return new SearchDtos.AccountsPage(
                    List.of(), safePage, safeSize, false);
        }

        List<SearchDtos.Account> found = jdbc.query("""
                SELECT u.id, u.username, u.user_type,
                       coalesce(
                         s.full_name,
                         c.company_name,
                         u.username
                       ) AS name,
                       f.storage_key
                FROM usuario u
                LEFT JOIN students s ON s.id = u.id
                LEFT JOIN company c ON c.id = u.id
                LEFT JOIN file f ON f.id = u.avatar_file_id
                                AND f.visibility = 'PUBLIC'
                WHERE position(:username IN u.username) > 0
                ORDER BY
                    CASE WHEN u.username = :username THEN 0 ELSE 1 END,
                    u.username,
                    u.id
                LIMIT :limit OFFSET :offset
                """, Map.of(
                        "username", term,
                        "limit", safeSize + 1,
                        "offset", (long) safePage * safeSize),
                (rs, row) -> {
                    String key = rs.getString("storage_key");

                    return new SearchDtos.Account(
                            rs.getObject("id", UUID.class),
                            rs.getString("username"),
                            rs.getString("name"),
                            UserType.valueOf(
                                    rs.getString("user_type")),
                            key == null || key.isBlank()
                                    ? null
                                    : storage.publicUrl(key));
                });

        return new SearchDtos.AccountsPage(
                found.stream().limit(safeSize).toList(),
                safePage,
                safeSize,
                found.size() > safeSize);
    }

    @Transactional(readOnly = true)
    public FeedPageDTO posts(
            UUID viewerId,
            String username,
            int page,
            int size) {
        String term = normalize(username, true);
        int safePage = safePage(page);
        int safeSize = safeSize(size);

        if (term.isEmpty()) {
            return new FeedPageDTO(
                    List.of(), safePage, safeSize, false);
        }

        List<UUID> found = jdbc.query("""
                SELECT p.id
                FROM post p
                JOIN usuario u ON u.id = p.author_id
                WHERE u.username = :username
                  AND p.deleted_at IS NULL
                ORDER BY p.created_at DESC, p.id DESC
                LIMIT :limit OFFSET :offset
                """, Map.of(
                        "username", term,
                        "limit", safeSize + 1,
                        "offset", (long) safePage * safeSize),
                (rs, row) -> rs.getObject("id", UUID.class));

        List<UUID> ids =
                found.stream().limit(safeSize).toList();

        Map<UUID, FeedItemDTO> details =
                feedItems.findItems(ids, viewerId);

        return new FeedPageDTO(
                ids.stream()
                        .map(details::get)
                        .filter(Objects::nonNull)
                        .toList(),
                safePage,
                safeSize,
                found.size() > safeSize);
    }

    private static String normalize(
            String raw,
            boolean exact) {
        String term = raw == null
                ? ""
                : raw.strip().toLowerCase(Locale.ROOT);

        if (term.startsWith("@")) {
            term = term.substring(1);
        }

        if (term.isEmpty()) return "";

        if (!term.matches(exact
                ? "[a-z0-9_.]{3,30}"
                : "[a-z0-9_.]{1,30}")) {
            throw new IllegalArgumentException(exact
                    ? "Introduza o username completo (3 a 30 caracteres)."
                    : "Pesquise por username: letras, números, _ e . "
                      + "(máximo 30 caracteres).");
        }

        return term;
    }

    private static int safePage(int page) {
        return Math.min(Math.max(page, 0), 100_000);
    }

    private static int safeSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }
}