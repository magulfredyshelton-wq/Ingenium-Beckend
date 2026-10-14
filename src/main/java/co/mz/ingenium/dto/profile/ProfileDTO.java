package co.mz.ingenium.dto.profile;

import co.mz.ingenium.dto.Feed.FeedTagDTO;

import java.util.List;
import java.util.UUID;

public record ProfileDTO(
        UUID id,
        String username,
        String type,
        String email,
        String name,
        String subtitle,
        String avatarKey,
        UUID courseId,
        String courseName,
        String fieldStudyName,
        String activityAreaName,
        String bio,
        ContactDTO contact,
        List<FeedTagDTO> tags,
        long postsCount,
        long followersCount,
        long followingCount,
        boolean followedByMe
) {}