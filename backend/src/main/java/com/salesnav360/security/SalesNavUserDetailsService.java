package com.salesnav360.security;

import com.salesnav360.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesNavUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public SalesNavUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByUsernameAndEnabledTrue(username)
                .map(SalesNavUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("SalesNav360 account was not found or is disabled."));
    }
}

