package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing Apigee organization information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeOrgInfo {
    private String name;
    private List<String> environments;
    private String displayName;
    private String description;
    private String type; // trial, paid, etc.
    private String createdAt;
    private String lastModifiedAt;
}
