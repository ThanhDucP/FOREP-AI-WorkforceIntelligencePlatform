package com.aiworkforce.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {
    @NotBlank(message = "Tên tổ chức không được để trống")
    private String name;
    private String domain;
    private String logoUrl;
    private Double latitude;
    private Double longitude;
    private Integer allowedRadiusMeters;
}
