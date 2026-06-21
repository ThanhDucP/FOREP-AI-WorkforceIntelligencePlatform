package com.aiworkforce.auth.service;

import com.aiworkforce.auth.dto.AuthResponse;
import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.core.enums.AccountStatus;
import com.aiworkforce.core.exception.BusinessException;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Role;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import com.aiworkforce.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2SocialAuthService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse loginWithOAuth2(String registrationId, Map<String, Object> attributes) {
        SocialProfile profile = SocialProfile.from(registrationId, attributes);
        if (profile.providerUserId() == null || profile.providerUserId().isBlank()) {
            throw new BusinessException("OAuth2 provider did not return a stable user id");
        }

        Account account = resolveAccount(profile);
        account = linkProviderData(account, profile);
        account = accountRepository.save(account);
        Account linkedAccount = account;

        Employee employee = employeeRepository.findByAccountId(linkedAccount.getId()).orElseGet(() -> {
            Employee created = new Employee();
            created.setAccount(linkedAccount);
            return created;
        });
        updateEmployeeProfile(employee, profile);
        employeeRepository.save(employee);

        UserDetails userDetails = new User(
                account.getEmail(),
                account.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole().getName().name()))
        );

        String jwtToken = jwtService.generateToken(userDetails);
        return AuthResponse.builder()
                .token(jwtToken)
                .refreshToken(jwtToken)
                .type("Bearer")
                .role(account.getRole().getName().name())
                .build();
    }

    private Account resolveAccount(SocialProfile profile) {
        Optional<Account> linkedAccount = switch (profile.provider()) {
            case GOOGLE -> accountRepository.findByGoogleId(profile.providerUserId());
            case GITHUB -> accountRepository.findByGithubId(profile.providerUserId());
            case JIRA -> accountRepository.findByJiraId(profile.providerUserId());
        };

        if (linkedAccount.isPresent()) {
            return linkedAccount.get();
        }

        if (profile.email() != null) {
            Optional<Account> accountByEmail = accountRepository.findByEmail(profile.email());
            if (accountByEmail.isPresent()) {
                return accountByEmail.get();
            }
        }

        Role defaultRole = roleRepository.findByName(RoleType.EMPLOYEE)
                .orElseThrow(() -> new BusinessException("Default role not found"));

        Account newAccount = new Account();
        newAccount.setEmail(profile.email() != null ? profile.email() : profile.syntheticEmail());
        newAccount.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newAccount.setRole(defaultRole);
        newAccount.setStatus(AccountStatus.ACTIVE);
        newAccount.setActive(true);
        newAccount.setLocked(false);
        return newAccount;
    }

    private Account linkProviderData(Account account, SocialProfile profile) {
        if (profile.provider() == SocialProfile.Provider.GOOGLE && account.getGoogleId() == null) {
            account.setGoogleId(profile.providerUserId());
        }
        if (profile.provider() == SocialProfile.Provider.GITHUB && account.getGithubId() == null) {
            account.setGithubId(profile.providerUserId());
        }
        if (profile.provider() == SocialProfile.Provider.JIRA && account.getJiraId() == null) {
            account.setJiraId(profile.providerUserId());
        }

        if (account.getAvatarUrl() == null || account.getAvatarUrl().isBlank()) {
            account.setAvatarUrl(profile.avatarUrl());
        }

        if (account.getRole() == null) {
            Role defaultRole = roleRepository.findByName(RoleType.EMPLOYEE)
                    .orElseThrow(() -> new BusinessException("Default role not found"));
            account.setRole(defaultRole);
        }

        if (account.getPassword() == null || account.getPassword().isBlank()) {
            account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }

        return account;
    }

    private void updateEmployeeProfile(Employee employee, SocialProfile profile) {
        if (employee.getFirstName() == null || employee.getFirstName().isBlank()) {
            employee.setFirstName(profile.firstName());
        }
        if (employee.getLastName() == null || employee.getLastName().isBlank()) {
            employee.setLastName(profile.lastName());
        }
        if (employee.getAvatarInitials() == null || employee.getAvatarInitials().isBlank()) {
            employee.setAvatarInitials(profile.avatarInitials());
        }
    }

    private record SocialProfile(
            Provider provider,
            String providerUserId,
            String email,
            String firstName,
            String lastName,
            String avatarUrl,
            String avatarInitials
    ) {
        enum Provider {
            GOOGLE,
            GITHUB,
            JIRA
        }

        static SocialProfile from(String registrationId, Map<String, Object> attributes) {
            Provider provider = Provider.valueOf(registrationId.toUpperCase());
            return switch (provider) {
                case GOOGLE -> fromGoogle(attributes);
                case GITHUB -> fromGithub(attributes);
                case JIRA -> fromJira(attributes);
            };
        }

        private static SocialProfile fromGoogle(Map<String, Object> attributes) {
            String providerUserId = stringValue(attributes, "sub");
            String email = stringValue(attributes, "email");
            String firstName = stringValue(attributes, "given_name");
            String lastName = stringValue(attributes, "family_name");
            String displayName = stringValue(attributes, "name");
            String avatarUrl = stringValue(attributes, "picture");

            NameParts parts = splitNames(firstName, lastName, displayName, email);
            return new SocialProfile(Provider.GOOGLE, providerUserId, email, parts.firstName(), parts.lastName(), avatarUrl, initials(parts.firstName(), parts.lastName(), displayName, email));
        }

        private static SocialProfile fromGithub(Map<String, Object> attributes) {
            String providerUserId = stringValue(attributes, "id");
            String login = stringValue(attributes, "login");
            String displayName = stringValue(attributes, "name");
            String email = stringValue(attributes, "email");
            String avatarUrl = stringValue(attributes, "avatar_url");

            if (email == null || email.isBlank()) {
                email = login != null ? login + "@users.noreply.github.com" : "github-" + providerUserId + "@users.noreply.github.com";
            }

            NameParts parts = splitNames(null, null, displayName != null ? displayName : login, email);
            return new SocialProfile(Provider.GITHUB, providerUserId, email, parts.firstName(), parts.lastName(), avatarUrl, initials(parts.firstName(), parts.lastName(), displayName != null ? displayName : login, email));
        }

        private static SocialProfile fromJira(Map<String, Object> attributes) {
            String providerUserId = firstStringValue(attributes, "account_id", "accountId", "sub");
            String displayName = firstStringValue(attributes, "name", "displayName");
            String email = firstStringValue(attributes, "email", "emailAddress");
            String avatarUrl = firstStringValue(attributes, "picture", "avatarUrl");

            if (email == null || email.isBlank()) {
                email = "jira-" + providerUserId + "@users.noreply.atlassian.com";
            }

            NameParts parts = splitNames(null, null, displayName, email);
            return new SocialProfile(Provider.JIRA, providerUserId, email, parts.firstName(), parts.lastName(), avatarUrl, initials(parts.firstName(), parts.lastName(), displayName, email));
        }

        private static NameParts splitNames(String firstName, String lastName, String displayName, String email) {
            if ((firstName != null && !firstName.isBlank()) || (lastName != null && !lastName.isBlank())) {
                return new NameParts(blankToNull(firstName), blankToNull(lastName));
            }

            if (displayName != null && !displayName.isBlank()) {
                String trimmed = displayName.trim();
                int spaceIndex = trimmed.indexOf(' ');
                if (spaceIndex > 0) {
                    return new NameParts(trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1).trim());
                }
                return new NameParts(trimmed, null);
            }

            if (email != null && email.contains("@")) {
                String localPart = email.substring(0, email.indexOf('@'));
                return new NameParts(localPart, null);
            }

            return new NameParts(null, null);
        }

        private static String initials(String firstName, String lastName, String displayName, String email) {
            String first = firstName != null && !firstName.isBlank() ? firstName : null;
            String last = lastName != null && !lastName.isBlank() ? lastName : null;

            if (first == null && displayName != null && !displayName.isBlank()) {
                String[] parts = displayName.trim().split("\\s+");
                if (parts.length > 0) {
                    first = parts[0];
                }
                if (parts.length > 1) {
                    last = parts[parts.length - 1];
                }
            }

            StringBuilder initials = new StringBuilder();
            if (first != null && !first.isBlank()) {
                initials.append(Character.toUpperCase(first.charAt(0)));
            }
            if (last != null && !last.isBlank()) {
                initials.append(Character.toUpperCase(last.charAt(0)));
            }

            if (initials.length() == 0 && email != null && email.contains("@")) {
                String localPart = email.substring(0, email.indexOf('@'));
                if (!localPart.isBlank()) {
                    initials.append(Character.toUpperCase(localPart.charAt(0)));
                }
            }

            return initials.length() > 0 ? initials.toString() : null;
        }

        private static String stringValue(Map<String, Object> attributes, String key) {
            Object value = attributes.get(key);
            return value != null ? String.valueOf(value).trim() : null;
        }

        private static String firstStringValue(Map<String, Object> attributes, String... keys) {
            for (String key : keys) {
                String value = stringValue(attributes, key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }

        private record NameParts(String firstName, String lastName) {
        }

        private String syntheticEmail() {
            return switch (provider) {
                case GOOGLE -> "google-" + providerUserId + "@users.noreply.google.com";
                case GITHUB -> "github-" + providerUserId + "@users.noreply.github.com";
                case JIRA -> "jira-" + providerUserId + "@users.noreply.atlassian.com";
            };
        }
    }
}
