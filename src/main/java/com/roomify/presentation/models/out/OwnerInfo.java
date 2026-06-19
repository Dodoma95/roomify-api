package com.roomify.presentation.models.out;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record OwnerInfo(
        @NonNull Long id,
        @NonNull String firstName,
        @NonNull String lastName,
        @NonNull String email,
        @Nullable String description,
        @Nullable String avatarUrl
) {}
