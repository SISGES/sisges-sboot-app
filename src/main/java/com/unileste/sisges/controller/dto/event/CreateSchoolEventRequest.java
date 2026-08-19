package com.unileste.sisges.controller.dto.event;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateSchoolEventRequest {
    @NotBlank @Size(max = 255) private String title;
    @Size(max = 2000) private String description;
    @NotNull @Future private LocalDateTime eventAt;
    @NotBlank @Pattern(regexp = "ALL|TEACHERS|CLASS") private String audience;
    private Integer classId;
}
