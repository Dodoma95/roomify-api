package com.roomify.shared.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class UtilsRequest {

    public static String normalizeEmail(@NonNull String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }

    public static String normalizeName(@NonNull String name) {
        return capitalize(name.trim().replaceAll("\\s+", " "));
    }

    public static String capitalize(@NonNull String value) {
        return Arrays.stream(value.split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static String normalizeAddress(@NonNull String address) {
        String normalized = address.trim().toLowerCase();
        normalized = normalized.replaceAll("\\s+", " ");
        // suppression des accents
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // suppression ponctuation
        return normalized.replaceAll("[,.]", "");
    }

}
