package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperAppResponse {
    private String appId;
    private String name;
    private String developerId;
    private String developerEmail;
    private List<String> products;
    private Map<String, String> attributes;
}
