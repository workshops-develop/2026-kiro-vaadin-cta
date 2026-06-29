package com.example.fundraiser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service that validates a hexadecimal key by POSTing it to the configured
 * validation API endpoint.
 *
 * <p>Satisfies Requirements 9.1–9.5, 10.3, 10.4:
 * <ul>
 *   <li>9.1 – makes a POST request to the Validation_API</li>
 *   <li>9.2 – sends JSON payload {"key": "..."}</li>
 *   <li>9.3 – honours the 1-second timeout configured on the injected RestTemplate</li>
 *   <li>9.4 – timeout / network errors map to FAIL (never propagated)</li>
 *   <li>9.5 – parses the JSON response {"result": boolean}</li>
 *   <li>10.3 – timeout → FAIL</li>
 *   <li>10.4 – any API failure → FAIL</li>
 * </ul>
 */
@Service
public class ValidationService {

    private final RestTemplate restTemplate;

    @Value("${validation.api.url:http://localhost:8080/api/validate}")
    private String apiUrl;

    public ValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Validates the given hex key against the remote validation API.
     *
     * @param hexKey the 240-character hexadecimal string to validate
     * @return {@link ValidationResult#OK} when the API returns {@code {"result": true}},
     *         {@link ValidationResult#FAIL} for {@code {"result": false}}, a null response,
     *         or any exception (timeout, connection error, non-2xx status, malformed JSON)
     */
    public ValidationResult validate(String hexKey) {
        try {
            ValidationResponse response = restTemplate.postForObject(
                    apiUrl, new ValidationRequest(hexKey), ValidationResponse.class);
            return (response != null && response.result())
                    ? ValidationResult.OK
                    : ValidationResult.FAIL;
        } catch (RestClientException e) {
            // RestClientException is the common superclass of ResourceAccessException
            // (timeout / network error) and HttpStatusCodeException (non-2xx status).
            // Catching it here covers all three cases. Never propagate — always return FAIL.
            return ValidationResult.FAIL;
        }
    }
}
