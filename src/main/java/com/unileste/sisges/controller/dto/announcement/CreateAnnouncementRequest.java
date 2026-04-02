package com.unileste.sisges.controller.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateAnnouncementRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255)
    private String title;

    @Size(max = 320)
    private String content;

    @NotBlank(message = "Tipo é obrigatório")
    @Pattern(regexp = "^(TEXT|IMAGE)$", message = "Tipo deve ser TEXT ou IMAGE")
    private String type;

    private String imagePath;

    private List<String> targetRoles;

    private List<String> hiddenForRoles;

    private LocalDateTime activeFrom;
    private LocalDateTime activeUntil;
}
