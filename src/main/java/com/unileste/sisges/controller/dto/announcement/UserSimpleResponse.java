package com.unileste.sisges.controller.dto.announcement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleResponse {

    private Integer id;
    private String name;
}
