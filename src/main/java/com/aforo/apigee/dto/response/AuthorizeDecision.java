package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizeDecision {
    private Boolean allow;
    private String reason;
    private Long subscriptionId;
    private Long ratePlanId;
}
