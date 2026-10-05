package com.chamcong.security;

import com.chamcong.entity.Employee;
import com.chamcong.repository.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Locale;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public DatabaseUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

        Employee employee = employeeRepository
                .findByUsername(normalizedUsername)
                .filter(Employee::isActive)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Không tìm thấy tài khoản: " + username
                        )
                );

        return User.builder()
                .username(employee.getUsername())
                .password(employee.getPasswordHash())
                .authorities(
                        Collections.singletonList(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + employee.getRole()
                                )
                        )
                )
                .build();
    }
}