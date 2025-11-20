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
public class CustomerSyncResponse {
    private String message;
    private int totalFetched;
    private int totalCreated;
    private int totalUpdated;
    private int totalFailed;
    private List<String> errors;
}
