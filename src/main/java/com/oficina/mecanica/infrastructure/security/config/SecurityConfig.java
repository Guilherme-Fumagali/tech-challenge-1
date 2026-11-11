package com.oficina.mecanica.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.infrastructure.security.RespostaProblemaJson;
import com.oficina.mecanica.infrastructure.security.filter.ChaveInternaFilter;
import com.oficina.mecanica.infrastructure.security.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ChaveInternaFilter chaveInternaFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          @Value("${app.seguranca.chave-interna:}") String chaveInterna,
                          ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.chaveInternaFilter = new ChaveInternaFilter(chaveInterna, objectMapper);
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ordens/*/status").permitAll()
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ordens", "/api/ordens/*", "/api/ordens/*/historico",
                    "/api/ordens/*/acompanhamento", "/api/veiculos", "/api/veiculos/*")
                    .hasAnyRole("CLIENTE", "FUNCIONARIO")
                .requestMatchers("/api/**").hasRole("FUNCIONARIO")
                .anyRequest().authenticated()
            )
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint((request, response, excecao) -> RespostaProblemaJson.escrever(objectMapper,
                    request, response, HttpStatus.UNAUTHORIZED, "Autenticação obrigatória."))
                .accessDeniedHandler((request, response, excecao) -> RespostaProblemaJson.escrever(objectMapper,
                    request, response, HttpStatus.FORBIDDEN, "Acesso não permitido para o papel do usuário.")))
            .addFilterBefore(chaveInternaFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

}
