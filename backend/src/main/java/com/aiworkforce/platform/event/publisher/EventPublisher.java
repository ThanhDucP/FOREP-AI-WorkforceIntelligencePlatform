package com.aiworkforce.platform.event.publisher;

import com.aiworkforce.platform.event.entity.WorkloadEvent;
import com.aiworkforce.platform.event.repository.WorkloadEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final WorkloadEventRepository workloadEventRepository;

    public void publish(WorkloadEvent event) {
        log.info("Publishing event: {}", event.getType());
        workloadEventRepository.save(event);
        applicationEventPublisher.publishEvent(event);
    }
}
