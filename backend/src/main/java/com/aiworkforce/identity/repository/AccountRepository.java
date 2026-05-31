package com.aiworkforce.identity.repository;
import com.aiworkforce.identity.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);
    Optional<Account> findByGoogleId(String googleId);
    Optional<Account> findByGithubId(String githubId);
    boolean existsByEmail(String email);
    long countByActive(boolean active);
}
