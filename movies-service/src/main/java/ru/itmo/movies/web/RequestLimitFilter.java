package ru.itmo.movies.web;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.error.Error;

@WebFilter(urlPatterns = "/*")
public class RequestLimitFilter implements Filter {

    private static final long MAX_BODY_BYTES = 1024L * 1024L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        boolean withBody = "POST".equals(req.getMethod()) || "PUT".equals(req.getMethod());
        if (withBody && req.getContentLengthLong() > MAX_BODY_BYTES) {
            res.setStatus(413);
            res.setContentType(MediaType.APPLICATION_JSON);
            res.getWriter().write(Json.write(new Error(413, ErrorCode.INVALID_PARAMETER, "Тело запроса превышает 1 МБ")));
            return;
        }
        chain.doFilter(request, response);
    }
}
