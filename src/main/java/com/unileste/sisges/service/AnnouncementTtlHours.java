package com.unileste.sisges.service;

import java.util.Set;

public final class AnnouncementTtlHours {

    public static final Set<Integer> ALLOWED = Set.of(1, 4, 10, 24, 48, 168);

    private AnnouncementTtlHours() {
    }

    public static boolean isAllowed(Integer ttlHours) {
        return ttlHours != null && ALLOWED.contains(ttlHours);
    }
}
