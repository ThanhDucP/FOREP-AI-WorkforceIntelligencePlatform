package com.aiworkforce.integration.dto;

import lombok.Data;

@Data
public class IntegrationConnectResponse {
    private String configId;
    private boolean webhookRegistered;
    private String webhookUrl;
    private String webhookSecret;
    private String message;
}
