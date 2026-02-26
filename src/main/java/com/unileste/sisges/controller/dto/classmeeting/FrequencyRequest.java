package com.unileste.sisges.controller.dto.classmeeting;

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
public class FrequencyRequest {

    @NotEmpty(message = "Lista de frequências é obrigatória")
    @Valid
    private List<FrequencyEntryRequest> entries;
}
