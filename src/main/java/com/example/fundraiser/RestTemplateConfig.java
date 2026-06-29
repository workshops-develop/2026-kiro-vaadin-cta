package com.example.fundraiser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for the RestTemplate used by ValidationService.
 * Configures a 5-second connect and read timeout to satisfy Requirement 9.3.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate validationRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds
        factory.setReadTimeout(5000);     // 5 seconds
        return new RestTemplate(factory);
    }
}
