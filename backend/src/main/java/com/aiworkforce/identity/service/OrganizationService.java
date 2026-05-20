package com.aiworkforce.identity.service;

import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.OrganizationRequest;
import com.aiworkforce.identity.dto.OrganizationResponse;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrganizationResponse getOrganizationById(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với ID: " + id));
        return mapToResponse(org);
    }

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request) {
        Organization org = new Organization();
        org.setName(request.getName());
        org.setDomain(request.getDomain());
        org.setLogoUrl(request.getLogoUrl());
        org.setLatitude(request.getLatitude());
        org.setLongitude(request.getLongitude());
        org.setAllowedRadiusMeters(request.getAllowedRadiusMeters());
        
        Organization saved = organizationRepository.save(org);
        return mapToResponse(saved);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với ID: " + id));
        
        org.setName(request.getName());
        org.setDomain(request.getDomain());
        org.setLogoUrl(request.getLogoUrl());
        org.setLatitude(request.getLatitude());
        org.setLongitude(request.getLongitude());
        org.setAllowedRadiusMeters(request.getAllowedRadiusMeters());
        
        Organization saved = organizationRepository.save(org);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteOrganization(UUID id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với ID: " + id));
        organizationRepository.delete(org);
    }

    public OrganizationResponse mapToResponse(Organization org) {
        if (org == null) return null;
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .domain(org.getDomain())
                .logoUrl(org.getLogoUrl())
                .latitude(org.getLatitude())
                .longitude(org.getLongitude())
                .allowedRadiusMeters(org.getAllowedRadiusMeters())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
}
