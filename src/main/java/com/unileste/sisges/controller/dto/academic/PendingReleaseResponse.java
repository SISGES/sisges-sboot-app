package com.unileste.sisges.controller.dto.academic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingReleaseResponse {

    private Integer trimester;
    private List<String> teachers;
}

