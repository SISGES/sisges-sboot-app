package com.unileste.sisges.job;

import com.unileste.sisges.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnnouncementExpiryJob {

    private final AnnouncementService announcementService;

    @Scheduled(fixedRate = 60_000)
    public void purgeExpiredAnnouncements() {
        try {
            announcementService.purgeExpiredAnnouncements();
        } catch (Exception e) {
            log.warn("Failed to purge expired announcements: {}", e.getMessage());
        }
    }
}
