package com.roomify.shared.utils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class UriBuilder {

    public static String build(
            @NonNull String baseUrl,
            @NonNull String basePath,
            @Nullable MultiValueMap<String, String> queryParams) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(basePath)
                .queryParams(queryParams)
                .build()
                .toUriString();
    }

}
