package com.aiworkforce.core.config;

import com.aiworkforce.core.enums.RoleType;
import com.aiworkforce.identity.entity.Account;
import com.aiworkforce.identity.entity.Employee;
import com.aiworkforce.identity.entity.Role;
import com.aiworkforce.identity.repository.AccountRepository;
import com.aiworkforce.identity.repository.EmployeeRepository;
import com.aiworkforce.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@forep.local";
        
        if (accountRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Seeding default ADMIN account...");
            
            Optional<Role> adminRoleOpt = roleRepository.findByName(RoleType.ADMIN);
            if (adminRoleOpt.isPresent()) {
                Account adminAccount = new Account();
                adminAccount.setEmail(adminEmail);
                adminAccount.setPassword(passwordEncoder.encode("admin"));
                adminAccount.setRole(adminRoleOpt.get());
                adminAccount.setActive(true);
                adminAccount.setLocked(false);
                adminAccount.setFailedLoginAttempts(0);
                
                adminAccount = accountRepository.save(adminAccount);
                
                Employee adminEmployee = new Employee();
                adminEmployee.setAccount(adminAccount);
                adminEmployee.setFirstName("System");
                adminEmployee.setLastName("Admin");
                adminEmployee.setJobTitle("Administrator");
                
                employeeRepository.save(adminEmployee);
                
                log.info("Default ADMIN account created successfully. Email: {}, Password: {}", adminEmail, "admin");
            } else {
                log.error("ADMIN role not found! Cannot seed default admin account.");
            }
        }
    }
}
