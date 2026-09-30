package com.cesarmillan.kairos.tvmaze;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TvMazeConfiguration {

    @Bean
    RestClient tvMazeRestClient(
            @Value("${tvmaze.base-url}") String baseUrl,
            @Value("${tvmaze.connect-timeout}") Duration connectTimeout,
            @Value("${tvmaze.read-timeout}") Duration readTimeout) {
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "Kairos/1.0")
                .build();
    }
}
