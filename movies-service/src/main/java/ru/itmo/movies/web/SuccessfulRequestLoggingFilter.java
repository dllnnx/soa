package ru.itmo.movies.web;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class SuccessfulRequestLoggingFilter extends OncePerRequestFilter {

    private final String instanceName;

    public SuccessfulRequestLoggingFilter(@Value("${eureka.instance.hostname}") String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator") || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        chain.doFilter(request, response);

        int status = response.getStatus();
        if (status >= 200 && status < 300) {
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info("HTTP {} {} -> {} ({} ms) instance={}",
                    request.getMethod(), request.getRequestURI(), status, durationMillis, instanceName);
        }
    }
}
