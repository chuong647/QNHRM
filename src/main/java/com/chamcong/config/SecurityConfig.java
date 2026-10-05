package com.chamcong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/health", "/error").permitAll()
                        .requestMatchers("/nhan-vien/**").hasRole("ADMIN")
                        .requestMatchers("/cham-cong/luu", "/cham-cong/xoa", "/cham-cong/xuat-excel").hasRole("ADMIN")
                        .requestMatchers("/cham-cong/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/doi-mat-khau", "/doi-mat-khau/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/ung-luong/luu", "/ung-luong/xoa", "/ung-luong/trang-thai").hasRole("ADMIN")
                        .requestMatchers("/ung-luong/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/bao-cao", "/bao-cao/**").hasRole("ADMIN")
                        .requestMatchers("/phieu-luong", "/phieu-luong/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("QNHRM_SESSION")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)
                        )
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                );

        return http.build();
    }
}
