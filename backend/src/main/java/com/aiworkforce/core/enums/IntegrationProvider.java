package com.aiworkforce.core.enums;

/**
 * Enum defining the source provider of a Task.
 */
public enum IntegrationProvider {
    INTERNAL, // Created directly in FOREP
    GITHUB,   // Synced from GitHub Issues
    JIRA,     // Synced from Jira
    GMAIL     // Extracted from Gmail (Phase 2)
}
