package com.unileste.sisges.controller.dto.classmeeting;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassMeetingRequest {

    @NotNull(message = "Data da aula é obrigatória")
    private LocalDate date;

    @NotNull(message = "ID da disciplina é obrigatório")
    private Integer disciplineId;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Horário de término é obrigatório")
    private LocalTime endTime;

    @NotNull(message = "ID da turma é obrigatório")
    private Integer classId;
}
