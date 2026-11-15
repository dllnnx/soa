package ru.itmo.movies.web;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.error.Error;

@Component
@AllArgsConstructor
public class RequestLimitFilter extends OncePerRequestFilter {

    private static final long MAX_BODY_BYTES = 1024L * 1024L;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean withBody = HttpMethod.POST.matches(request.getMethod()) || HttpMethod.PUT.matches(request.getMethod());
        if (withBody && contentLength(request) > MAX_BODY_BYTES) {
            response.setStatus(413);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    new Error(413, ErrorCode.INVALID_PARAMETER, "Тело запроса превышает 1 МБ"));
            return;
        }
        chain.doFilter(request, response);
    }

    private static long contentLength(HttpServletRequest request) {
        String value = request.getHeader("Content-Length");
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
