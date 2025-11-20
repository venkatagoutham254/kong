package com.aforo.apigee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingResponse {
    private Long subscriptionId;
    private Long ratePlanId;
    private String appId;
    private Boolean wroteBackAttributes;
}
