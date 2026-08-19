package com.unileste.sisges.controller.dto.activity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FillActivityGradesRequest {

    @Valid
    @NotEmpty(message = "Informe pelo menos uma nota")
    private List<GradeEntryRequest> entries;
}

