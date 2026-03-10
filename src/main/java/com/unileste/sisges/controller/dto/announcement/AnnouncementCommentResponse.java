package com.unileste.sisges.controller.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementCommentResponse {

    private Integer id;
    private String content;
    private LocalDateTime createdAt;
    private UserSimpleResponse user;
}
