package com.unileste.sisges.controller.dto.announcement;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UpdateAnnouncementRequest {

    @Size(max = 255)
    private String title;

    private String content;

    @Pattern(regexp = "^(TEXT|IMAGE)?$", message = "Tipo deve ser TEXT ou IMAGE")
    private String type;

    private String imagePath;

    private List<String> targetRoles;
    private List<String> hiddenForRoles;

    private LocalDateTime activeFrom;
    private LocalDateTime activeUntil;
}
