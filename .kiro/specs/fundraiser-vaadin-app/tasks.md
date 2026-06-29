# Implementation Plan: Fundraiser Vaadin App

## Overview

Implement a Spring Boot 3.x + Vaadin 24 (Flow) single-page fundraiser call-to-action web application. The implementation proceeds bottom-up: pure utility classes first, then the service layer, then the mock API, then the Vaadin UI components, and finally wiring everything together with CSS theming and static assets.

## Tasks

- [x] 1. Set up Maven project structure and dependencies
  - Create a Spring Boot 3.x Maven project with `spring-boot-starter-web`, `vaadin-spring-boot-starter` (Vaadin 24), `spring-boot-starter-test`, `jqwik`, and `junit-jupiter` dependencies
  - Configure `pom.xml` with Java 17 source/target, the Vaadin Maven plugin, and the jqwik test engine
  - Create the standard Spring Boot main application class (`@SpringBootApplication`)
  - Create `src/main/resources/application.properties` with `validation.api.url=http://localhost:8080/api/validate`
  - Create the Vaadin theme directory at `src/main/frontend/themes/fundraiser/` and an empty `styles.css` placeholder
  - Create the static resource directory `src/main/resources/META-INF/resources/images/` and add a placeholder `hero.jpg`
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. Implement data models and API records
  - [x] 2.1 Create `ValidationResult` enum
    - Create `ValidationResult.java` with values `OK` and `FAIL`
    - _Requirements: 10.1, 10.2_

  - [x] 2.2 Create `ValidationRequest` and `ValidationResponse` records
    - Create `ValidationRequest.java` as a Java record with a single `String key` field
    - Create `ValidationResponse.java` as a Java record with a single `boolean result` field
    - _Requirements: 9.2, 9.5_

  - [ ]* 2.3 Write property test for API payload serialization round-trip
    - **Property 7: API payload serialization preserves the key value**
    - Use jqwik `@Property(tries = 200)` with a `@Provide` method generating valid 240-char hex strings
    - Serialize a `ValidationRequest` to JSON with `ObjectMapper`, deserialize back, assert `key` field equality
    - **Validates: Requirements 9.2, 9.5**

- [x] 3. Implement `HexInputValidator` utility
  - [x] 3.1 Create `HexInputValidator` class
    - Define `REQUIRED_LENGTH = 240` and `HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{240}$")`
    - Implement `isValid(String input)`: returns `true` iff input is non-null and matches `HEX_PATTERN`
    - Implement `stripNonHex(String input)`: removes all non-`[0-9a-fA-F]` characters, then truncates to 240 chars
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

  - [ ]* 3.2 Write property test for hex string validation (Property 1)
    - **Property 1: Hex string validation accepts exactly valid 240-char hex strings**
    - Use `@Property(tries = 500)` with a `@Provide` method for valid 240-char hex strings; assert `isValid()` returns `true`
    - Use a second `@Property(tries = 500)` with a `@Provide` method for invalid strings (wrong length or non-hex chars); assert `isValid()` returns `false`
    - **Validates: Requirements 5.2, 5.3, 5.4, 5.5**

  - [ ]* 3.3 Write property test for `stripNonHex` (Property 4)
    - **Property 4: Non-hex character stripping preserves only hex characters**
    - Use `@Property(tries = 500)` with `@ForAll String input`; assert result matches `[0-9a-fA-F]{0,240}`
    - **Validates: Requirements 5.3, 5.4, 5.5**

  - [ ]* 3.4 Write unit tests for `HexInputValidator`
    - Test: valid 240-char hex string accepted; empty string rejected; 239-char string rejected; 241-char string rejected; string with non-hex chars rejected; null rejected
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

- [x] 4. Implement `HexStringGenerator` utility
  - [x] 4.1 Create `HexStringGenerator` class
    - Define `LENGTH = 240` and `HEX_CHARS = "0123456789abcdef"`
    - Implement `generate()` as a static method using `SecureRandom` to pick 240 characters from `HEX_CHARS`
    - Return a lowercase hex string of exactly 240 characters
    - _Requirements: 6.2, 6.4_

  - [ ]* 4.2 Write property test for generator output invariant (Property 2)
    - **Property 2: Generator always produces a 240-character lowercase hex string**
    - Use `@Property(tries = 200)`; call `HexStringGenerator.generate()`; assert length is 240 and string matches `[0-9a-f]{240}`
    - **Validates: Requirements 6.2, 6.4**

  - [ ]* 4.3 Write property test for generator/validator round-trip (Property 3)
    - **Property 3: Generator output is always accepted by the validator**
    - Use `@Property(tries = 200)`; call `HexStringGenerator.generate()`; assert `HexInputValidator.isValid()` returns `true`
    - **Validates: Requirements 6.2, 6.4, 5.2, 5.3**

  - [ ]* 4.4 Write unit tests for `HexStringGenerator`
    - Test: generated string is exactly 240 chars; all characters are lowercase hex digits
    - _Requirements: 6.2, 6.4_

- [x] 5. Checkpoint — Ensure all utility tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement `RestTemplateConfig` and `ValidationService`
  - [x] 6.1 Create `RestTemplateConfig` configuration class
    - Annotate with `@Configuration`
    - Define a `@Bean` method `validationRestTemplate()` that creates a `RestTemplate` backed by `SimpleClientHttpRequestFactory` with `connectTimeout = 5000` ms and `readTimeout = 5000` ms
    - _Requirements: 9.3_

  - [x] 6.2 Create `ValidationService` service class
    - Annotate with `@Service`; inject `RestTemplate` via constructor
    - Inject `validation.api.url` via `@Value("${validation.api.url:http://localhost:8080/api/validate}")`
    - Implement `validate(String hexKey)`: POST a `ValidationRequest` to `apiUrl`, parse the `ValidationResponse`, return `ValidationResult.OK` if `result` is `true`, otherwise `ValidationResult.FAIL`
    - Catch `ResourceAccessException`, `HttpStatusCodeException`, and `RestClientException`; return `ValidationResult.FAIL` for all exceptions — never propagate
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 10.3, 10.4_

  - [ ]* 6.3 Write property test for API failure mapping (Property 8)
    - **Property 8: Any API failure maps to FAIL result**
    - Use `@Property(tries = 100)` with a `@Provide` method generating representative `RuntimeException` subtypes (`ResourceAccessException`, `HttpClientErrorException`, `RestClientException`)
    - Mock `RestTemplate.postForObject()` to throw the provided exception; assert `validate()` returns `ValidationResult.FAIL`
    - **Validates: Requirements 9.4, 10.3, 10.4**

  - [ ]* 6.4 Write unit tests for `ValidationService`
    - Test: mock `RestTemplate` returns `{"result": true}` → `OK`; returns `{"result": false}` → `FAIL`; throws `ResourceAccessException` → `FAIL`; throws `HttpClientErrorException` → `FAIL`
    - _Requirements: 9.1, 9.4, 9.5, 10.1, 10.2_

- [x] 7. Implement `MockValidationController`
  - [x] 7.1 Create `MockValidationController` REST controller
    - Annotate with `@RestController` and `@RequestMapping("/api")`
    - Implement `POST /validate` handler: accept `@RequestBody ValidationRequest`, return `new ValidationResponse(false)`
    - _Requirements: 11.1, 11.2, 11.3_

  - [ ]* 7.2 Write property test for mock always returning false (Property 9)
    - **Property 9: Mock API always returns false for any input**
    - Use `@Property(tries = 200)` with a `@Provide` method generating valid 240-char hex strings
    - Instantiate `MockValidationController` directly; call `validate(new ValidationRequest(key))`; assert `response.result()` is `false`
    - **Validates: Requirements 11.1, 11.2, 11.3**

  - [ ]* 7.3 Write unit tests for `MockValidationController`
    - Test: POST with valid payload returns HTTP 200 and `{"result": false}`
    - _Requirements: 11.1, 11.3_

- [x] 8. Checkpoint — Ensure all service and controller tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement Vaadin UI components
  - [x] 9.1 Create `HeroSection` component
    - Extend `Div`; call `setWidthFull()`
    - Add an `Image` component pointing to `images/hero.jpg` with an accessible alt text
    - Apply a CSS class `hero-section` for height and object-fit styling
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 9.2 Create `TextBlock` component
    - Extend `Div`; constructor accepts `String htmlContent`
    - Call `setWidthFull()`; apply CSS class `text-block`; set inner HTML via `getElement().setProperty("innerHTML", htmlContent)`
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 9.3 Create `HexValidatorComponent`
    - Extend `VerticalLayout`; inject `ValidationService` via constructor
    - Call `setWidthFull()`, `setAlignItems(Alignment.CENTER)`, and `addClassName("hex-validator")` to centre the panel
    - Create `TextArea keyInput` with `setWidthFull()`, `setMaxLength(240)`, `setLabel("Hex Key")`, `setAriaLabel("Hexadecimal key input")`, and a `ValueChangeListener` that calls `HexInputValidator.stripNonHex()` and sets the filtered value back
    - Set client-side pattern attribute: `keyInput.getElement().setAttribute("pattern", "[0-9a-fA-F]*")`
    - Create `Button randomGeneratorButton` labeled "Generate Random Key"; on click call `HexStringGenerator.generate()` and set `keyInput.setValue(result)`
    - Create `Button checkButton` labeled "Check Key" with `setAriaLabel("Validate hex key")`, `addThemeVariants(ButtonVariant.LUMO_PRIMARY)`, and `addClassName("check-btn-wrapper")` (enables overlay spinner positioning)
    - Create `Span resultDisplay` with `setVisible(false)` initially and `addClassName("result-display")`
    - Create `HorizontalLayout buttonRow` with `setAlignItems(Alignment.CENTER)` containing `checkButton` and `resultDisplay` side by side
    - Add components in order: `keyInput`, `randomGeneratorButton`, `buttonRow`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 7.1, 8.1, 10.5_

  - [x] 9.4 Implement `HexValidatorComponent` check button click handler
    - On click: if `keyInput` is empty, show inline error and return; if `HexInputValidator.isValid()` returns `false`, show inline error and return
    - Set `checkButton.setEnabled(false)` and `checkButton.addClassName("loading")` — the CSS `::after` overlay spinner covers the button face
    - Call `ValidationService.validate()` on a background thread; use `UI.getCurrent().access()` for the UI update after completion
    - On completion: set `resultDisplay` text to "OK" or "FAIL", apply CSS class `result-ok` or `result-fail`, set `resultDisplay.setVisible(true)`, restore `checkButton.setEnabled(true)`, call `checkButton.removeClassName("loading")`
    - _Requirements: 7.2, 7.3, 7.4, 8.1, 8.3, 8.4, 8.5, 10.1, 10.2, 10.5_

  - [ ]* 9.5 Write property test for invalid input never calling the service (Property 5)
    - **Property 5: Invalid key input always produces an error, never a validation call**
    - Use `@Property(tries = 300)` with a `@Provide` method generating strings that fail `HexInputValidator.isValid()`
    - Mock `ValidationService`; invoke the check button click handler with invalid input; assert the mock was never called and an error is displayed
    - **Validates: Requirements 7.3, 7.4**

  - [ ]* 9.6 Write property test for button state restoration (Property 6)
    - **Property 6: Button state is always restored after validation completes**
    - Use `@Property(tries = 100)` with a `@Provide` method generating `ValidationResult` values (`OK`, `FAIL`)
    - Mock `ValidationService.validate()` to return the provided result; trigger the check button; assert `checkButton.isEnabled()` is `true` after completion
    - **Validates: Requirements 8.2, 8.3, 8.4**

  - [ ]* 9.7 Write unit tests for `HexValidatorComponent`
    - Test: empty input shows error and does not call service; invalid input shows error and does not call service; valid input calls service; result display shows "OK" in green for `OK`; result display shows "FAIL" in red for `FAIL`
    - _Requirements: 7.2, 7.3, 7.4, 10.1, 10.2, 10.5_

- [x] 10. Implement `MainView` and wire all components together
  - [x] 10.1 Create `MainView` Vaadin route
    - Annotate with `@Route("")` and `@PageTitle("Fundraiser")`
    - Extend `VerticalLayout`; call `setWidthFull()`, `setPadding(false)`, `setSpacing(false)`
    - Inject `ValidationService` via constructor
    - Add `new HeroSection()`, one or more `new TextBlock(htmlContent)` instances with fundraiser copy, and `new HexValidatorComponent(validationService)` as children
    - _Requirements: 1.3, 2.1, 3.1, 4.1, 4.2, 4.3_

  - [x] 10.2 Apply Vaadin theme and CSS styling
    - Annotate `AppShell` with `@Theme("fundraiser")` (moved from `MainView` per Vaadin 24 rules)
    - In `frontend/themes/fundraiser/styles.css`, add styles for:
      - `.hero-section`: `width: 100%; height: 60vh; overflow: hidden;` and `img { width: 100%; height: 100%; object-fit: cover; }`
      - `.text-block`: `width: 100%; padding: 2rem; box-sizing: border-box;` with fundraiser-appropriate typography and colors
      - `.hex-validator`: `max-width: 600px; margin: auto; padding: 2rem; align-items: center;` to centre the validator panel
      - `.check-btn-wrapper`: `position: relative;` to enable the overlay spinner
      - `.loading::after`: absolutely-positioned spinning circle overlay covering the button face; `@keyframes btn-spin` rotation animation
      - `.loading::part(label)`: `opacity: 0` to hide the button label while the spinner is active
      - `.result-display`: `font-size: 1.1rem; font-weight: bold; margin-left: 0.75rem;` for inline result next to the button
      - `.result-ok`: `color: green; font-weight: bold;`
      - `.result-fail`: `color: red; font-weight: bold;`
      - Responsive breakpoints ensuring `TextBlock` and `HexValidatorComponent` remain readable on mobile
    - _Requirements: 2.3, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 8.1, 10.5_

  - [x] 10.3 Enable server push for immediate UI updates
    - Add `@Push` annotation to `AppShell` (the `AppShellConfigurator` class)
    - This enables WebSocket-based server push so that `ui.access()` calls from the background validation thread are delivered to the browser immediately when the API responds or times out
    - Without `@Push`, the wait animation would never stop automatically — the browser only receives UI updates on the next user interaction
    - _Requirements: 8.6, 8.7_

- [x] 11. Write Spring Boot integration tests
  - [ ]* 11.1 Write `@SpringBootTest` context load test
    - Assert the Spring application context loads without errors
    - _Requirements: 1.1, 1.2_

  - [ ]* 11.2 Write `MockMvc` integration test for `POST /api/validate`
    - Use `@SpringBootTest` + `@AutoConfigureMockMvc`; POST `{"key": "aaa..."}` (240 chars) to `/api/validate`; assert HTTP 200 and response body `{"result":false}`
    - _Requirements: 11.1, 11.2, 11.3_

- [x] 12. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at logical boundaries
- Property tests (Properties 1–9) validate universal correctness guarantees using jqwik
- Unit tests validate specific examples and edge cases
- The mock controller (`MockValidationController`) is intentionally in the same application; swap `validation.api.url` in `application.properties` to point to a real backend when ready
