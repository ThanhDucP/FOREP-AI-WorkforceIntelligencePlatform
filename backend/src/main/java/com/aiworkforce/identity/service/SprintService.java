package com.aiworkforce.identity.service;

import com.aiworkforce.core.enums.SprintStatus;
import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.SprintResponse;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.entity.Sprint;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final OrganizationRepository organizationRepository;

    public List<SprintResponse> getAllSprints() {
        return sprintRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SprintResponse getSprintById(UUID id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));
        return mapToResponse(sprint);
    }

    public List<SprintResponse> getSprintsByOrganization(UUID organizationId) {
        return sprintRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SprintResponse getActiveSprint() {
        Sprint sprint = sprintRepository.findFirstByStatusOrderBySprintNumberDesc(SprintStatus.ACTIVE)
                .orElseGet(() -> sprintRepository.findTopByOrderBySprintNumberDesc()
                        .orElse(null));
        return mapToResponse(sprint);
    }

    public SprintResponse getActiveSprintByOrganization(UUID organizationId) {
        Sprint sprint = sprintRepository.findFirstByOrganizationIdAndStatusOrderBySprintNumberDesc(organizationId, SprintStatus.ACTIVE)
                .orElse(null);
        return mapToResponse(sprint);
    }

    @Transactional
    public SprintResponse createSprint(SprintResponse request) {
        Sprint sprint = new Sprint();
        sprint.setSprintNumber(request.getSprintNumber());
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setCommittedStoryPoints(request.getCommittedStoryPoints() != null ? request.getCommittedStoryPoints() : 0);
        sprint.setCompletedStoryPoints(0);
        sprint.setVelocityConfidence(request.getVelocityConfidence() != null ? request.getVelocityConfidence() : 0.85);
        sprint.setStatus(request.getStatus() != null ? request.getStatus() : SprintStatus.PLANNING);
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + request.getOrganizationId()));
            sprint.setOrganization(organization);
        }
        
        if (sprint.getStatus() == SprintStatus.ACTIVE) {
            // Deactivate any other active sprint
            sprintRepository.findFirstByStatusOrderBySprintNumberDesc(SprintStatus.ACTIVE).ifPresent(active -> {
                active.setStatus(SprintStatus.COMPLETED);
                sprintRepository.save(active);
            });
        }

        return mapToResponse(sprintRepository.save(sprint));
    }

    @Transactional
    public SprintResponse updateSprint(UUID id, SprintResponse request) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));
        
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setCommittedStoryPoints(request.getCommittedStoryPoints());
        sprint.setCompletedStoryPoints(request.getCompletedStoryPoints());
        sprint.setVelocityConfidence(request.getVelocityConfidence());
        if (request.getOrganizationId() != null) {
            Organization organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + request.getOrganizationId()));
            sprint.setOrganization(organization);
        } else {
            sprint.setOrganization(null);
        }
        
        if (request.getStatus() != null && request.getStatus() != sprint.getStatus()) {
            if (request.getStatus() == SprintStatus.ACTIVE) {
                // Deactivate any other active sprint
                sprintRepository.findFirstByStatusOrderBySprintNumberDesc(SprintStatus.ACTIVE).ifPresent(active -> {
                    active.setStatus(SprintStatus.COMPLETED);
                    sprintRepository.save(active);
                });
            }
            sprint.setStatus(request.getStatus());
        }

        return mapToResponse(sprintRepository.save(sprint));
    }

    @Transactional
    public void deleteSprint(UUID id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint not found with id: " + id));
        sprintRepository.delete(sprint);
    }

    public SprintResponse mapToResponse(Sprint sprint) {
        if (sprint == null) return null;
        return SprintResponse.builder()
                .id(sprint.getId())
                .sprintNumber(sprint.getSprintNumber())
                .name(sprint.getName())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .committedStoryPoints(sprint.getCommittedStoryPoints())
                .completedStoryPoints(sprint.getCompletedStoryPoints())
                .velocityConfidence(sprint.getVelocityConfidence())
                .status(sprint.getStatus())
                .organizationId(sprint.getOrganization() != null ? sprint.getOrganization().getId() : null)
                .organizationName(sprint.getOrganization() != null ? sprint.getOrganization().getName() : null)
                .build();
    }
}
