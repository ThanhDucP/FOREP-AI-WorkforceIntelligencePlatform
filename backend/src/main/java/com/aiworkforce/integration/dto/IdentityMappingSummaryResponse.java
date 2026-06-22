package com.aiworkforce.integration.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdentityMappingSummaryResponse {
    private long totalMembers;
    private long matched;
    private long possibleMatch;
    private long unmatched;
    private long conflict;
}