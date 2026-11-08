package com.sfhotels.booking.config;

import com.sfhotels.booking.client.HotelClientErrorHandler;
import com.sfhotels.booking.security.TokenService;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HotelClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder());
    }

    @Bean
    public RestClient hotelRestClient(@LoadBalanced RestClient.Builder builder, HotelClientProperties properties,
                                      TokenService tokenService, HotelClientErrorHandler errorHandler) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenService.issueServiceToken());
                    return execution.execute(request, body);
                })
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, errorHandler)
                .build();
    }
}
