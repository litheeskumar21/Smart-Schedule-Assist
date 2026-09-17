package com.smartschedule.config;

import com.smartschedule.security.CustomAuthenticationProvider;
import com.smartschedule.security.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final CustomAuthenticationProvider customAuthenticationProvider;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler, CustomAuthenticationProvider customAuthenticationProvider) {
        this.loginSuccessHandler = loginSuccessHandler;
        this.customAuthenticationProvider = customAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(customAuthenticationProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/forgot-password", "/reset-password", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/teacher/**").hasRole("TEACHER")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/error/403"));

        return http.build();
    }

    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
