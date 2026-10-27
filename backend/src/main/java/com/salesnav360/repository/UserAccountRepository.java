package com.salesnav360.repository;

import com.salesnav360.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameAndEnabledTrue(String username);
    boolean existsByUsernameAndEnabledTrue(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);

    Page<UserAccount> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String email, Pageable pageable);

    @Query("select count(distinct u.id) from UserAccount u join u.roles r where u.enabled = true and r.name = :role")
    long countEnabledUsersWithRole(@Param("role") String role);
}

