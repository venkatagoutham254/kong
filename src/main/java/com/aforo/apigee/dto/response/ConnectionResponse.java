package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {
    private Boolean connected;
    private String message;
    private String organizationName;  // Apigee organization name
    private String hmacSecret;  // Auto-generated HMAC secret for webhooks
}
