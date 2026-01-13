package com.aforo.integration.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * DTO representing an Apigee API Proxy.
 * Maps to Kong Service concept.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApigeeApiProxy {
    private String name;
    private List<String> basepaths;
    private String revision;
    private String latestRevision;
    private List<String> targetEndpoints;
    private List<String> proxyEndpoints;
    private Map<String, Object> metadata;
    private String createdAt;
    private String lastModifiedAt;
}
