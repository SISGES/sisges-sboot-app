package com.unileste.sisges.controller.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private Integer id;
    private String title;
    private String content;
    private String type;
    private String imagePath;
    private List<String> targetRoles;
    private List<String> hiddenForRoles;
    private LocalDateTime activeFrom;
    private LocalDateTime activeUntil;
    private LocalDateTime createdAt;
    private long likeCount;
    private boolean likedByCurrentUser;
    private long commentCount;
}
