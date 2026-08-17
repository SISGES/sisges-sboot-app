package com.unileste.sisges.controller.dto.academic;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartAcademicYearRequest {

    @NotNull(message = "Data final do ano letivo é obrigatória")
    private LocalDate yearEndDate;
}

