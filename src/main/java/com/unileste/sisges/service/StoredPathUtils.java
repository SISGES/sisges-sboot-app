package com.unileste.sisges.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StoredPathUtils {

    private StoredPathUtils() {
    }

    public static String extractStorageKey(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        for (String prefix : new String[] {"announcements/", "profiles/", "materials/", "activities/", "general/"}) {
            int idx = storedPath.indexOf(prefix);
            if (idx >= 0) {
                return normalizeStorageKey(storedPath.substring(idx));
            }
        }
        return null;
    }

    public static String normalizeStorageKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        if (normalized.contains("..") || normalized.contains("\\") || !normalized.contains("/")) {
            return null;
        }
        return normalized;
    }
}
