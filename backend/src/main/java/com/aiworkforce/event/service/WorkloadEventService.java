package com.aiworkforce.event.service;

import com.aiworkforce.event.entity.WorkloadEvent;
import com.aiworkforce.event.repository.WorkloadEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadEventService {
    
    private final WorkloadEventRepository workloadEventRepository;
    
    public WorkloadEvent recordEvent(WorkloadEvent event) {
        return workloadEventRepository.save(event);
    }
}
