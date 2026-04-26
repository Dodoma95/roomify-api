package com.roomify.presentation.models.out;

public record PageInfo(
        int page,
        int pageSize,
        int totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {}
