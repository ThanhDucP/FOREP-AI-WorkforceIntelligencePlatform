package com.aiworkforce.identity.service;

import com.aiworkforce.core.exception.ResourceNotFoundException;
import com.aiworkforce.identity.dto.OrganizationRequest;
import com.aiworkforce.identity.dto.OrganizationResponse;
import com.aiworkforce.identity.entity.Organization;
import com.aiworkforce.identity.repository.OrganizationRepository;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        organization.setAddress("123 Business Street, Hanoi");

        request = new OrganizationRequest();
        request.setName("FOREP Corp Updated");
        request.setDomain("forep.local");
        request.setAddress("456 Insight Avenue, Hanoi");
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
        assertEquals("456 Insight Avenue, Hanoi", response.getAddress());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    public void testDeleteOrganization_Success() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        organizationService.deleteOrganization(orgId);

        verify(organizationRepository, times(1)).delete(organization);
    }
}
