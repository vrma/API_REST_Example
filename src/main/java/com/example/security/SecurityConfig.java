package com.example.security;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SECURED_URLs = {"/productos/**"};

    private static final String[] UN_SECURED_URLs = {
            "/users/**"
    };    


    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf().disable();
    http.authorizeHttpRequests()
            .requestMatchers(UN_SECURED_URLs).permitAll().and()
            .authorizeHttpRequests().requestMatchers(SECURED_URLs)
            .hasAuthority("ADMIN").anyRequest()
            .authenticated().and().httpBasic(withDefaults());

        return http.build();

    }

    // http.csrf().disable();
    // http.authorizeHttpRequests()
    //         .requestMatchers(UN_SECURED_URLs).permitAll().and()
    //         .authorizeHttpRequests().requestMatchers(SECURED_URLs)
    //         .hasAuthority("ADMIN").anyRequest()
    //         .authenticated().and().httpBasic(withDefaults());    
    
    // public static void main(String[] args) {
    //     System.out.println(new SecurityConfig().passwordEncoder().encode("123456"));
    // }
}
