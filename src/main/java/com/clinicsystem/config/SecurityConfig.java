package com.clinicsystem.config;

import com.clinicsystem.security.JwtAuthFilter;
import com.clinicsystem.security.RateLimiter;
import com.clinicsystem.security.RateLimitFilter;
import com.clinicsystem.security.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimiter rateLimiter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(this.userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Constructed inline (not a @Bean) so Spring Boot does not also register
        // it as a servlet filter — otherwise it would run twice per request.
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimitProperties, objectMapper, rateLimiter);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // OpenAPI / Swagger UI
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()

                        // Public auth
                        .requestMatchers("/auth/**").permitAll()

                        // Public doctor discovery (patients need this before booking)
                        .requestMatchers(HttpMethod.GET, "/doctors", "/doctors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search/**").permitAll()

                        // Public available slots (authenticated booking still required to reserve)
                        .requestMatchers(HttpMethod.GET, "/schedules/doctor/**").permitAll()

                        // Doctor manages own schedule
                        .requestMatchers("/schedules/**").hasAnyRole("DOCTOR", "ADMIN")

                        // Appointments: patients book/cancel; doctors confirm/reject/complete
                        .requestMatchers("/appointments/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN")

                        // Medical records: patient + doctor (ownership enforced in service)
                        .requestMatchers("/medical-records/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN")

                        // Admin only
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) ->
                        response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
