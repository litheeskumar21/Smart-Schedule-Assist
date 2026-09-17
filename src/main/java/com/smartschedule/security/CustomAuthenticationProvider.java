package com.smartschedule.security;

import com.smartschedule.model.User;
import com.smartschedule.model.enums.UserRole;
import com.smartschedule.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final HttpServletRequest request;

    public CustomAuthenticationProvider(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, HttpServletRequest request) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.request = request;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName() != null ? authentication.getName().trim() : "";
        String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
        
        String selectedRole = request.getParameter("role");
        if (selectedRole == null || selectedRole.isBlank()) {
            selectedRole = request.getParameter("selectedRole");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid ID/Username or Password. Please try again."));

        if (!user.isEnabled()) {
            throw new DisabledException("Your account is currently disabled. Please contact the administrator.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid ID/Username or Password. Please try again.");
        }

        // Validate selected role against actual user role
        if (selectedRole != null && !selectedRole.trim().isEmpty()) {
            String roleClean = selectedRole.trim().toLowerCase();
            UserRole actualRole = user.getRole();

            if ("student".equals(roleClean) && actualRole != UserRole.ROLE_STUDENT) {
                throw new BadCredentialsException("The entered ID does not belong to a Student account. Please select the correct tab.");
            } else if ("teacher".equals(roleClean) && actualRole != UserRole.ROLE_TEACHER) {
                throw new BadCredentialsException("The entered ID does not belong to a Teacher account. Please select the correct tab.");
            } else if ("admin".equals(roleClean) && actualRole != UserRole.ROLE_ADMIN) {
                throw new BadCredentialsException("The entered ID does not belong to an Admin account. Please select the correct tab.");
            }
        }

        return new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
