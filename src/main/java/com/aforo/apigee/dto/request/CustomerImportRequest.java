package com.aforo.apigee.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerImportRequest {
    private String businessEmail;
    private String firstName;
    private String lastName;
    private String companyName;
    private String phoneNumber;
    private String source;
    private String externalId;
}
