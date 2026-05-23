package com.aiworkforce.identity.service;

import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.OrganizationRequest;
import com.aiworkforce.identity.dto.OrganizationResponse;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization organization;
    private OrganizationRequest request;
    private UUID orgId;

    @BeforeEach
    public void setUp() {
        orgId = UUID.randomUUID();
        organization = new Organization();
        organization.setId(orgId);
        organization.setName("FOREP Corp");
        organization.setDomain("forep.local");
        organization.setLatitude(21.0285);
        organization.setLongitude(105.8521);
        organization.setAllowedRadiusMeters(200);

        request = new OrganizationRequest();
        request.setName("FOREP Corp Updated");
        request.setDomain("forep.local");
        request.setLatitude(21.0300);
        request.setLongitude(105.8500);
        request.setAllowedRadiusMeters(300);
    }

    @Test
    public void testGetAllOrganizations() {
        when(organizationRepository.findAll()).thenReturn(Collections.singletonList(organization));

        List<OrganizationResponse> responses = organizationService.getAllOrganizations();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("FOREP Corp", responses.get(0).getName());
    }

    @Test
    public void testGetOrganizationById_Success() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        OrganizationResponse response = organizationService.getOrganizationById(orgId);

        assertNotNull(response);
        assertEquals("FOREP Corp", response.getName());
    }

    @Test
    public void testGetOrganizationById_NotFound() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            organizationService.getOrganizationById(orgId);
        });
    }

    @Test
    public void testCreateOrganization() {
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        OrganizationResponse response = organizationService.createOrganization(request);

        assertNotNull(response);
        assertEquals("FOREP Corp", response.getName()); // Mock repository returns the predefined mock
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    public void testUpdateOrganization_Success() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.updateOrganization(orgId, request);

        assertNotNull(response);
        assertEquals("FOREP Corp Updated", response.getName());
        assertEquals(300, response.getAllowedRadiusMeters());
        assertEquals(21.0300, response.getLatitude());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    public void testDeleteOrganization_Success() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        organizationService.deleteOrganization(orgId);

        verify(organizationRepository, times(1)).delete(organization);
    }
}
