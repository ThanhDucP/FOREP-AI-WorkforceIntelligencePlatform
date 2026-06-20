package com.aiworkforce.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {
    private int totalFetched;
    private int totalCreated;
    private int totalUpdated;

    public static SyncResult empty() {
        return SyncResult.builder().totalFetched(0).totalCreated(0).totalUpdated(0).build();
    }

    public void addFetched(int count) {
        this.totalFetched += count;
    }

    public void addCreated() {
        this.totalCreated += 1;
    }

    public void addUpdated() {
        this.totalUpdated += 1;
    }

    public void merge(SyncResult other) {
        if (other == null) return;
        this.totalFetched += other.totalFetched;
        this.totalCreated += other.totalCreated;
        this.totalUpdated += other.totalUpdated;
    }
}