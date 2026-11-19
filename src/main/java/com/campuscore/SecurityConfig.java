package com.campuscore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Two security chains:
 * 1) /admin/** has its own sign in page (/admin/login) and only the ADMIN role.
 * 2) Everything else is the student and faculty portal with /login.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin", "/admin/**")
            .authorizeHttpRequests(a -> a
                .requestMatchers("/admin/login").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .formLogin(f -> f
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .successHandler(adminSuccess())
                .failureUrl("/admin/login?error")
                .permitAll())
            .exceptionHandling(e -> e.accessDeniedPage("/denied"))
            .logout(l -> l.logoutUrl("/admin/logout").logoutSuccessUrl("/admin/login?logout"));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain portalChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/denied", "/error", "/style.css", "/admin.css", "/student-form.js", "/assignment-form.js",
                                 "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/faculty/**").hasRole("FACULTY")
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .successHandler(portalSuccess())
                .failureUrl("/login?error")
                .permitAll())
            .exceptionHandling(e -> e.accessDeniedPage("/denied"))
            .logout(l -> l.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    /** Only an admin may sign in at /admin/login. Anyone else is signed out and refused. */
    private AuthenticationSuccessHandler adminSuccess() {
        return (request, response, auth) -> {
            if (isAdmin(auth)) {
                response.sendRedirect(request.getContextPath() + "/admin");
            } else {
                new SecurityContextLogoutHandler().logout(request, response, auth);
                response.sendRedirect(request.getContextPath() + "/admin/login?error");
            }
        };
    }

    /** Students and faculty sign in at /login. An admin is signed out and refused. */
    private AuthenticationSuccessHandler portalSuccess() {
        return (request, response, auth) -> {
            if (isAdmin(auth)) {
                new SecurityContextLogoutHandler().logout(request, response, auth);
                response.sendRedirect(request.getContextPath() + "/login?error");
            } else {
                response.sendRedirect(request.getContextPath() + RoleRedirect.target(auth.getAuthorities()));
            }
        };
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
