package com.unileste.sisges.controller.dto.schoolclass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchoolClassRequest {

    @NotBlank(message = "Nome da turma é obrigatório")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Série é obrigatória")
    @Size(max = 30)
    @Pattern(
        regexp = "^(1º ano - Fundamental|2º ano - Fundamental|3º ano - Fundamental|4º ano - Fundamental|5º ano - Fundamental|6º ano|7º ano|8º ano|9º ano|1º ano - Médio|2º ano - Médio|3º ano - Médio)$",
        message = "Série inválida"
    )
    private String academicYear;

    private List<Integer> studentIds;

    private List<Integer> teacherIds;

    private List<Integer> disciplineIds;
}