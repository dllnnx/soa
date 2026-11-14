package ru.itmo.movies.web;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import ru.itmo.movies.error.ErrorCode;

@Provider
@PreMatching
public class RequestLimitFilter implements ContainerRequestFilter {

    private static final long MAX_BODY_BYTES = 1024L * 1024L;

    @Override
    public void filter(ContainerRequestContext request) {
        String method = request.getMethod();
        boolean withBody = HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method);
        if (withBody && contentLength(request) > MAX_BODY_BYTES) {
            request.abortWith(ErrorResponses.of(
                    413, ErrorCode.INVALID_PARAMETER, "Тело запроса превышает 1 МБ"));
        }
    }

    private static long contentLength(ContainerRequestContext request) {
        String value = request.getHeaderString(HttpHeaders.CONTENT_LENGTH);
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
