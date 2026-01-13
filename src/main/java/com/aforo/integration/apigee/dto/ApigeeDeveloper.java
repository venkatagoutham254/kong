package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representing an Apigee Developer.
 * Maps to part of Kong Consumer concept.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeDeveloper {
    @JsonProperty("developerId")
    private String developerId;
    
    private String email;
    private String firstName;
    private String lastName;
    private String userName;
    private String organizationName;
    private String status;
    private List<String> apps;
    private Map<String, String> attributes;
    private String createdAt;
    private String lastModifiedAt;
}
