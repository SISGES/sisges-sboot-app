package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.announcement.AnnouncementResponse;
import com.unileste.sisges.controller.dto.announcement.CreateAnnouncementRequest;
import com.unileste.sisges.model.Announcement;
import com.unileste.sisges.model.User;
import com.unileste.sisges.repository.AnnouncementRepository;
import com.unileste.sisges.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private UserRepository userRepository;
    @Mock private AnnouncementLikeService announcementLikeService;
    @Mock private AnnouncementCommentService announcementCommentService;
    @Mock private FeedNotificationService feedNotificationService;
    @Mock private StorageService storageService;

    @InjectMocks
    private AnnouncementService announcementService;

    @Test
    void create_persistsAnnouncement_withTtl_andBroadcasts() {
        User teacher = User.builder().id(5).name("Teacher").email("t@test.sisges.local").userRole("TEACHER").build();
        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .title("Aviso teste")
                .content("Conteúdo")
                .type("TEXT")
                .ttlHours(10)
                .build();

        when(userRepository.findById(5)).thenReturn(Optional.of(teacher));
        when(announcementRepository.save(any())).thenAnswer(inv -> {
            Announcement a = inv.getArgument(0);
            a.setId(12);
            return a;
        });
        when(announcementLikeService.countLikes(12)).thenReturn(0L);
        when(announcementLikeService.isLikedByUser(12, 5)).thenReturn(false);
        when(announcementCommentService.countComments(12)).thenReturn(0L);

        AnnouncementResponse response = announcementService.create(request, 5);

        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement saved = captor.getValue();
        assertNotNull(saved.getActiveFrom());
        assertNotNull(saved.getActiveUntil());
        assertEquals(saved.getActiveFrom().plusHours(10), saved.getActiveUntil());

        assertEquals(12, response.getId());
        assertEquals("Aviso teste", response.getTitle());
        verify(feedNotificationService).broadcast("ANNOUNCEMENT_CREATED", 12);
    }

    @Test
    void findActiveForRole_returnsFeedForStudent() {
        Announcement announcement = Announcement.builder()
                .id(1)
                .title("Feed item")
                .content("Body")
                .type("TEXT")
                .build();

        when(announcementRepository.findActiveForRole(eq("STUDENT"), any(LocalDateTime.class))).thenReturn(List.of(announcement));
        when(announcementLikeService.countLikes(1)).thenReturn(1L);
        when(announcementLikeService.isLikedByUser(1, 3)).thenReturn(true);
        when(announcementCommentService.countComments(1)).thenReturn(2L);

        List<AnnouncementResponse> feed = announcementService.findActiveForRole("STUDENT", 3);

        assertEquals(1, feed.size());
        assertEquals("Feed item", feed.get(0).getTitle());
        assertEquals(1L, feed.get(0).getLikeCount());
    }

    @Test
    void delete_hardDeletesAnnouncement_andStoredImage() {
        Announcement announcement = Announcement.builder()
                .id(7)
                .title("Expired")
                .type("IMAGE")
                .imagePath("https://cdn.example.com/announcements/abc.png")
                .build();

        when(announcementRepository.findById(7)).thenReturn(Optional.of(announcement));

        announcementService.delete(7);

        verify(storageService).delete("https://cdn.example.com/announcements/abc.png");
        verify(announcementRepository).delete(announcement);
        verify(feedNotificationService).broadcast("ANNOUNCEMENT_DELETED", 7);
    }

    @Test
    void purgeExpiredAnnouncements_hardDeletesEachExpiredRow() {
        Announcement expired = Announcement.builder()
                .id(3)
                .title("Old")
                .type("TEXT")
                .build();

        when(announcementRepository.findByDeletedAtIsNullAndActiveUntilBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        announcementService.purgeExpiredAnnouncements();

        verify(announcementRepository).delete(expired);
        verify(storageService, never()).delete(any());
        verify(feedNotificationService).broadcast("ANNOUNCEMENT_DELETED", 3);
    }
}
