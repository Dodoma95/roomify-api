package com.roomify.presentation.models.out;

import java.util.List;

import org.jspecify.annotations.NonNull;

public record PlacePage(
        @NonNull List<PlaceResponse> results,
        @NonNull PageInfo pageInfo
) {}
