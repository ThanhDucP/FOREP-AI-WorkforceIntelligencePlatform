package com.aiworkforce.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String phoneNumber;
    private String email;
    private UUID teamId;
    private String teamName;
}
