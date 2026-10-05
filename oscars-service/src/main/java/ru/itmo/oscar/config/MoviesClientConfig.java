package ru.itmo.oscar.config;

import java.time.Duration;
import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MoviesClientConfig {

    @Bean
    public RestClient moviesRestClient(
            RestClient.Builder builder,
            @Value("${movies.base-url}") String baseUrl,
            @Value("${movies.connect-timeout}") Duration connectTimeout,
            @Value("${movies.read-timeout}") Duration readTimeout) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
