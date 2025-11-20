package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiProductResponse {
    private String name;
    private String displayName;
    private String quota;
    private List<String> resources;
}
