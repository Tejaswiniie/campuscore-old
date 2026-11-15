package com.campuscore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/style.css", "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .requestMatchers("/faculty/**").hasRole("FACULTY")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").successHandler(roleRedirect()).permitAll())
            .logout(l -> l.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    private AuthenticationSuccessHandler roleRedirect() {
        return (request, response, auth) ->
            response.sendRedirect(request.getContextPath() + RoleRedirect.target(auth.getAuthorities()));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Demo users. Initial password is the same as the ID. A database replaces this later. */
    @Bean
    UserDetailsService users(PasswordEncoder enc) {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin").password(enc.encode("admin123")).roles("ADMIN").build(),
            User.withUsername("22CS001").password(enc.encode("22CS001")).roles("STUDENT").build(),
            User.withUsername("FAC001").password(enc.encode("FAC001")).roles("FACULTY").build());
    }
}
