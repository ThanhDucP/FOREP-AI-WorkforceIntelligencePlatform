package com.aiworkforce.integration.service;

import com.aiworkforce.integration.entity.TaskIntegrationConfig;

/**
 * Strategy interface for processing incoming webhooks from external providers.
 */
public interface WebhookProcessorStrategy {
    
    /**
     * Processes the webhook payload and creates/updates tasks accordingly.
     *
     * @param payload The raw JSON payload from the webhook
     * @param signature The signature header (if any) used for verification
     * @param config The integration config of the team this webhook belongs to
     */
    void processPayload(String payload, String signature, TaskIntegrationConfig config);
}
