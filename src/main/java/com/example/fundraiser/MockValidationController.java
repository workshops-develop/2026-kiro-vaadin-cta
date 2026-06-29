package com.example.fundraiser;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MockValidationController {

    @PostMapping("/validate")
    public ValidationResponse validate(@RequestBody ValidationRequest request) {
        return new ValidationResponse(false);
    }
}
