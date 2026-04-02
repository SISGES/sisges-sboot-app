package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.announcement.FeedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String eventType, Integer announcementId) {
        FeedEvent event = FeedEvent.builder()
                .type(eventType)
                .announcementId(announcementId)
                .build();
        messagingTemplate.convertAndSend("/topic/feed", event);
    }
}
