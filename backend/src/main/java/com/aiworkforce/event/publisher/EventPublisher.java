package com.aiworkforce.event.publisher;

import com.aiworkforce.event.entity.WorkloadEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventPublisher {
    
    private final ApplicationEventPublisher publisher;
    
    public void publishEvent(WorkloadEvent event) {
        publisher.publishEvent(event);
    }
}
