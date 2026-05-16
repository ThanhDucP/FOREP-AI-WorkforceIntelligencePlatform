package com.aiworkforce.event.processor;

import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.repository.WorkloadEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventProcessor {
    
    private final WorkloadEventRepository workloadEventRepository;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleWorkloadEvent(WorkloadEvent event) {
        log.info("Received workload event: {}", event.getEventType());
        // Calculate impact score based on event type dynamically
        int score = 0;
        switch(event.getEventType()) {
            case TASK_COMPLETED -> score = 10;
            case TASK_OVERDUE -> score = -5;
            case TASK_CREATED -> score = 2;
            case TASK_UPDATED -> score = 1;
            default -> score = 0;
        }
        event.setImpactScore(score);
        workloadEventRepository.save(event);
        log.info("Saved workload event with score {}", score);
        
        // TODO: Trigger live analytics cache update if necessary
    }
}
