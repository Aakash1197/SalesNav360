package com.salesnav360.config;

import com.salesnav360.entity.Role;
import com.salesnav360.entity.UserAccount;
import com.salesnav360.repository.RoleRepository;
import com.salesnav360.repository.UserAccountRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public BootstrapAdminRunner(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${salesnav.bootstrap.admin-username:}") String username,
            @Value("${salesnav.bootstrap.admin-email:}") String email,
            @Value("${salesnav.bootstrap.admin-password:}") String password) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userAccountRepository.count() > 0) {
            return;
        }
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "No SalesNav360 users exist. Set SALESNAV_BOOTSTRAP_ADMIN_USERNAME, "
                            + "SALESNAV_BOOTSTRAP_ADMIN_EMAIL, and SALESNAV_BOOTSTRAP_ADMIN_PASSWORD "
                            + "to create the first administrator.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("SALESNAV_BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters.");
        }
        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("The bootstrap administrator username is already in use.");
        }
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("The bootstrap administrator email is already in use.");
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        userAccountRepository.save(new UserAccount(username, email, passwordEncoder.encode(password), Set.of(adminRole)));
    }
}

