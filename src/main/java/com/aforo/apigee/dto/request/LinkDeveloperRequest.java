package com.aforo.apigee.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkDeveloperRequest {
    
    @NotBlank(message = "Aforo customer ID is required")
    private String aforoCustomerId;
}
