package com.aforo.apigee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApigeeCustomer {
    private String developerId;
    private String email;
    private String firstName;
    private String lastName;
    private String userName;
    private String organizationName;
    private String status;
}
