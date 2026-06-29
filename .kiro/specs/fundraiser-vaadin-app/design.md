# Design Document: Fundraiser Vaadin App

## Overview

This document describes the technical design for a Spring Boot + Vaadin single-page web application serving as a fundraiser call-to-action page. The application is entirely server-side rendered via Vaadin Flow, requires no backend database, and includes an interactive hexadecimal key validation component that communicates with a REST API.

**Key design decisions:**

- **Vaadin Flow (not Hilla/React)**: The server-side Java component model is the right fit here — no need for a separate frontend build pipeline, and the UI logic (input filtering, state management) is cleanest in Java.
- **RestTemplate with SimpleClientHttpRequestFactory**: Straightforward synchronous HTTP client with explicit timeout configuration. `WebClient` would add reactive complexity without benefit for a single blocking call.
- **Mock API as a Spring `@RestController`**: The mock endpoint lives in the same application, making it trivially easy to replace with a real external URL via configuration later.
- **Input filtering via `ValueChangeListener` + `setPattern`**: Vaadin's `TextArea` supports `setMaxLength` and client-side pattern enforcement, supplemented by a server-side `ValueChangeListener` for robust hex-only filtering.
- **`@Push` on `AppShell`**: Server push (WebSocket) is required so that `ui.access()` calls from the background validation thread are delivered to the browser immediately. Without `@Push`, the browser only receives UI updates on the next user interaction — meaning the wait animation would never stop automatically after the API response or timeout.

---

## Architecture

The application follows a simple layered structure within a single Spring Boot process:

```
┌─────────────────────────────────────────────────────────┐
│                    Browser (Vaadin Flow)                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  MainView (@Route(""))                            │   │
│  │  ├── HeroSection                                  │   │
│  │  ├── TextBlock (1..n)                             │   │
│  │  └── HexValidatorComponent                        │   │
│  │       ├── KeyInput (TextArea)                     │   │
│  │       ├── RandomGeneratorButton                   │   │
│  │       └── ButtonRow (HorizontalLayout)            │   │
│  │            ├── CheckButton (+ spinner overlay)    │   │
│  │            └── ResultDisplay                      │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
         │  HTTP POST /api/validate (JSON)
         ▼
┌─────────────────────────────────────────────────────────┐
│  ValidationService (Spring @Service)                     │
│  └── RestTemplate (5s timeout)                          │
└─────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  MockValidationController (@RestController)              │
│  POST /api/validate → {"result": false}                  │
└─────────────────────────────────────────────────────────┘
```

**Technology stack:**
- Java 17+
- Spring Boot 3.x
- Vaadin 24 (Flow)
- Maven (build)
- jqwik (property-based testing)
- JUnit 5 (unit/integration tests)

---

## Components and Interfaces

### AppShell

```java
@Push
@Theme("fundraiser")
public class AppShell implements AppShellConfigurator {
}
```

`@Push` enables WebSocket-based server push. This is essential for the wait animation to stop automatically: the background validation thread calls `ui.access()` after the API responds or times out, and `@Push` ensures that update is delivered to the browser immediately rather than waiting for the next user interaction.

`@Theme("fundraiser")` must live here (not on a `@Route` view) per Vaadin 24's `AppShellConfigurator` contract.

### MainView

```java
@Route("")
@PageTitle("Fundraiser")
public class MainView extends VerticalLayout {
    // Sets width to 100%, padding to 0, spacing to 0
    // Children: HeroSection, TextBlock(s), HexValidatorComponent
}
```

The root view uses `setWidthFull()` and removes default padding/spacing so child sections can span the full viewport width without gaps.

### HeroSection

```java
public class HeroSection extends Div {
    // Contains an Image component
    // setWidthFull(), fixed or viewport-relative height via CSS
}
```

Implemented as a `Div` with inline styles or a CSS class. The image is loaded from `src/main/resources/META-INF/resources/images/hero.jpg` (served as a static resource by Spring Boot).

### TextBlock

```java
public class TextBlock extends Div {
    // Constructor: TextBlock(String htmlContent)
    // setWidthFull(), CSS class "text-block"
}
```

A reusable full-width content section. Multiple instances are added to `MainView`. Styling is applied via `styles.css` in the Vaadin theme directory.

### HexValidatorComponent

```java
public class HexValidatorComponent extends VerticalLayout {
    private final TextArea keyInput;
    private final Button randomGeneratorButton;
    private final Button checkButton;
    private final Span resultDisplay;
    private final ValidationService validationService;

    // Constructor injects ValidationService
}
```

The central interactive component. It owns all UI state transitions (loading, result display) and delegates API calls to `ValidationService`.

**Constructor layout setup:**
- `setWidthFull()` — component spans the full parent width
- `setAlignItems(Alignment.CENTER)` — centres all child components horizontally
- `addClassName("hex-validator")` — applies the centering/max-width CSS panel styles

**Key_Input (`TextArea`):**
- `setWidthFull()` — text area spans the full width of the panel
- `setMaxLength(240)` — enforces hard character limit
- `addValueChangeListener` — strips non-hex characters on each change (server-side filter)
- Client-side pattern `[0-9a-fA-F]*` via `getElement().setAttribute("pattern", "[0-9a-fA-F]*")`

**Random_Generator button:**
- On click: calls `HexStringGenerator.generate()`, sets `keyInput.setValue(result)`

**Check_Button row (`HorizontalLayout`):**
- A `HorizontalLayout buttonRow` wraps `checkButton` and `resultDisplay` side by side with `setAlignItems(Alignment.CENTER)`
- `checkButton` has `addThemeVariants(ButtonVariant.LUMO_PRIMARY)` in the constructor and `addClassName("check-btn-wrapper")` to enable the overlay spinner positioning
- On click: validates input, disables button, adds CSS class `loading` (spinner overlay covers the button face), calls `ValidationService.validate()` asynchronously via `UI.access()`, then updates `resultDisplay`, restores `checkButton.setEnabled(true)`, and removes the `loading` class

**Spinner_Overlay:**
- Implemented purely in CSS using `.loading::after` — an absolutely-positioned pseudo-element centred over the button face with a rotating border animation
- No separate Java component needed; the `loading` class is added/removed on `checkButton`

**Result_Display (`Span`):**
- Placed inside `buttonRow` immediately after `checkButton` — visible in the same horizontal line
- Hidden by default (`setVisible(false)`)
- On result: sets text to "OK" or "FAIL", applies CSS color class `result-ok` or `result-fail`, sets visible

### HexStringGenerator

```java
public class HexStringGenerator {
    private static final int LENGTH = 240;
    private static final String HEX_CHARS = "0123456789abcdef";

    public static String generate() {
        // Uses SecureRandom to pick 240 chars from HEX_CHARS
        // Returns lowercase hex string of exactly 240 characters
    }
}
```

A pure static utility — no state, no dependencies. This makes it straightforward to test with property-based tests.

### HexInputValidator

```java
public class HexInputValidator {
    public static final int REQUIRED_LENGTH = 240;
    public static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{240}$");

    public static boolean isValid(String input) {
        return input != null && HEX_PATTERN.matcher(input).matches();
    }

    public static String stripNonHex(String input) {
        // Removes all non-hex characters, truncates to 240 chars
        return input.replaceAll("[^0-9a-fA-F]", "")
                    .substring(0, Math.min(input.replaceAll("[^0-9a-fA-F]", "").length(), REQUIRED_LENGTH));
    }
}
```

A pure utility class with no side effects — ideal for property-based testing.

### ValidationService

```java
@Service
public class ValidationService {
    private final RestTemplate restTemplate;

    @Value("${validation.api.url:http://localhost:8080/api/validate}")
    private String apiUrl;

    public ValidationResult validate(String hexKey) {
        // POST {"key": hexKey} to apiUrl
        // Returns ValidationResult.OK or ValidationResult.FAIL
        // Catches ResourceAccessException (timeout after 5s), HttpClientErrorException, etc.
    }
}
```

**`ValidationResult` enum:**
```java
public enum ValidationResult { OK, FAIL }
```

### RestTemplate Configuration

```java
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
```

### MockValidationController

```java
@RestController
@RequestMapping("/api")
public class MockValidationController {

    @PostMapping("/validate")
    public ValidationResponse validate(@RequestBody ValidationRequest request) {
        return new ValidationResponse(false);
    }

    public record ValidationRequest(String key) {}
    public record ValidationResponse(boolean result) {}
}
```

The mock lives in the same Spring Boot application. The `validation.api.url` property defaults to `http://localhost:8080/api/validate`, so no configuration change is needed during development.

---

## Data Models

### API Request Payload

```json
{ "key": "240hexcharacters..." }
```

Java record:
```java
public record ValidationRequest(String key) {}
```

### API Response Payload

```json
{ "result": true }
```

Java record:
```java
public record ValidationResponse(boolean result) {}
```

### Internal Result Enum

```java
public enum ValidationResult {
    OK,   // API returned {"result": true}
    FAIL  // API returned {"result": false}, timeout, or any error
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Hex string validation accepts exactly valid 240-char hex strings

*For any* string, `HexInputValidator.isValid()` returns `true` if and only if the string is exactly 240 characters long and every character is in `[0-9a-fA-F]`. Any string that is shorter, longer, or contains a non-hex character must return `false`.

**Validates: Requirements 5.2, 5.3, 5.4, 5.5**

---

### Property 2: Generator always produces a 240-character lowercase hex string

*For any* invocation of `HexStringGenerator.generate()`, the returned string is exactly 240 characters long and every character is in `[0-9a-f]` (lowercase only).

**Validates: Requirements 6.2, 6.4**

---

### Property 3: Generator output is always accepted by the validator

*For any* invocation of `HexStringGenerator.generate()`, the returned string must satisfy `HexInputValidator.isValid()`. This is a round-trip consistency property between the generator and the validator.

**Validates: Requirements 6.2, 6.4, 5.2, 5.3**

---

### Property 4: Non-hex character stripping preserves only hex characters

*For any* input string, `HexInputValidator.stripNonHex()` returns a string where every character is in `[0-9a-fA-F]` and the length is at most 240. The result is a prefix of the hex characters found in the original input.

**Validates: Requirements 5.3, 5.4, 5.5**

---

### Property 5: Invalid key input always produces an error, never a validation call

*For any* string that fails `HexInputValidator.isValid()` (wrong length or non-hex characters), clicking Check_Button must display an error message and must NOT invoke `ValidationService.validate()`.

**Validates: Requirements 7.3, 7.4**

---

### Property 6: Button state is always restored after validation completes

*For any* validation outcome (success, failure, timeout, or network error), after `ValidationService.validate()` returns, the Check_Button must be enabled and not in a loading state.

**Validates: Requirements 8.2, 8.3, 8.4**

---

### Property 7: API payload serialization preserves the key value

*For any* 240-character hex string `k`, serializing a `ValidationRequest(k)` to JSON and deserializing it back produces a `ValidationRequest` whose `key` field equals `k`.

**Validates: Requirements 9.2, 9.5**

---

### Property 8: Any API failure maps to FAIL result

*For any* exception thrown by the HTTP client (timeout, connection refused, malformed response, non-2xx status), `ValidationService.validate()` must return `ValidationResult.FAIL` and must not propagate the exception to the caller.

**Validates: Requirements 9.4, 10.3, 10.4**

---

### Property 9: Mock API always returns false for any input

*For any* `ValidationRequest` sent to `MockValidationController`, the response is always `{"result": false}` regardless of the key value.

**Validates: Requirements 11.1, 11.2, 11.3**

---

## Error Handling

| Scenario | Handling |
|---|---|
| Empty Key_Input on Check_Button click | Show inline error message; do not call API |
| Key_Input contains non-hex characters | `stripNonHex` filters on each keystroke; validator rejects on submit |
| Key_Input length ≠ 240 on submit | Show inline error message; do not call API |
| API timeout (> 5 seconds) | `ResourceAccessException` caught; `ValidationResult.FAIL` returned |
| API connection refused / network error | `ResourceAccessException` caught; `ValidationResult.FAIL` returned |
| API returns non-2xx status | `HttpStatusCodeException` caught; `ValidationResult.FAIL` returned |
| API returns malformed JSON | `RestClientException` caught; `ValidationResult.FAIL` returned |
| API returns `{"result": false}` | Normal path; `ValidationResult.FAIL` returned |

All exceptions from `ValidationService.validate()` are caught internally. The method always returns a `ValidationResult` — it never throws. This simplifies the UI layer to a simple enum switch.

---

## Testing Strategy

### Unit Tests (JUnit 5)

Focus on specific examples, edge cases, and component wiring:

- `HexInputValidatorTest`: valid 240-char hex strings accepted; empty string rejected; 239/241-char strings rejected; strings with non-hex chars rejected; null rejected
- `HexStringGeneratorTest`: generated string is 240 chars; all chars are lowercase hex
- `ValidationServiceTest`: mock `RestTemplate` returns true → `OK`; returns false → `FAIL`; throws `ResourceAccessException` → `FAIL`; throws `HttpClientErrorException` → `FAIL`
- `MockValidationControllerTest`: POST with valid payload returns `{"result": false}`
- `HexValidatorComponentTest`: empty input shows error; invalid input shows error; valid input triggers service call; result display shows "OK" in green for `OK`; shows "FAIL" in red for `FAIL`

### Property-Based Tests (jqwik)

Each property test runs a minimum of 100 iterations. Tests are tagged with the feature and property number.

**Property 1 — Hex string validation:**
```java
// Feature: fundraiser-vaadin-app, Property 1: Hex string validation accepts exactly valid 240-char hex strings
@Property(tries = 500)
void validHexStringsAreAccepted(@ForAll("validHexStrings") String s) {
    assertThat(HexInputValidator.isValid(s)).isTrue();
}

@Property(tries = 500)
void invalidStringsAreRejected(@ForAll("invalidHexStrings") String s) {
    assertThat(HexInputValidator.isValid(s)).isFalse();
}
```

**Property 2 — Generator output invariant:**
```java
// Feature: fundraiser-vaadin-app, Property 2: Generator always produces a 240-character lowercase hex string
@Property(tries = 200)
void generatorProduces240CharLowercaseHex() {
    String result = HexStringGenerator.generate();
    assertThat(result).hasSize(240);
    assertThat(result).matches("[0-9a-f]{240}");
}
```

**Property 3 — Generator/validator round-trip:**
```java
// Feature: fundraiser-vaadin-app, Property 3: Generator output is always accepted by the validator
@Property(tries = 200)
void generatorOutputPassesValidation() {
    assertThat(HexInputValidator.isValid(HexStringGenerator.generate())).isTrue();
}
```

**Property 4 — stripNonHex preserves only hex characters:**
```java
// Feature: fundraiser-vaadin-app, Property 4: Non-hex character stripping preserves only hex characters
@Property(tries = 500)
void stripNonHexProducesOnlyHexChars(@ForAll String input) {
    String result = HexInputValidator.stripNonHex(input);
    assertThat(result).matches("[0-9a-fA-F]{0,240}");
}
```

**Property 7 — Payload serialization round-trip:**
```java
// Feature: fundraiser-vaadin-app, Property 7: API payload serialization preserves the key value
@Property(tries = 200)
void payloadSerializationRoundTrip(@ForAll("validHexStrings") String key) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ValidationRequest req = new ValidationRequest(key);
    String json = mapper.writeValueAsString(req);
    ValidationRequest parsed = mapper.readValue(json, ValidationRequest.class);
    assertThat(parsed.key()).isEqualTo(key);
}
```

**Property 8 — Any API failure maps to FAIL:**
```java
// Feature: fundraiser-vaadin-app, Property 8: Any API failure maps to FAIL result
@Property(tries = 100)
void anyExceptionMapsToFail(@ForAll("apiExceptions") RuntimeException ex) {
    RestTemplate mockTemplate = mock(RestTemplate.class);
    when(mockTemplate.postForObject(any(), any(), any())).thenThrow(ex);
    ValidationService service = new ValidationService(mockTemplate);
    assertThat(service.validate("a".repeat(240))).isEqualTo(ValidationResult.FAIL);
}
```

**Property 9 — Mock always returns false:**
```java
// Feature: fundraiser-vaadin-app, Property 9: Mock API always returns false for any input
@Property(tries = 200)
void mockAlwaysReturnsFalse(@ForAll("validHexStrings") String key) {
    MockValidationController controller = new MockValidationController();
    ValidationResponse response = controller.validate(new ValidationRequest(key));
    assertThat(response.result()).isFalse();
}
```

### Integration Tests

- Spring Boot context loads without errors (`@SpringBootTest`)
- `POST /api/validate` with valid payload returns HTTP 200 and `{"result": false}`
- `ValidationService` with real `RestTemplate` and mock server (WireMock) handles timeout correctly

### CSS Theme (`styles.css`)

Key rules in `src/main/frontend/themes/fundraiser/styles.css`:

**`.hex-validator` — centred panel (Requirement 4.1, 4.2):**
```css
.hex-validator {
  max-width: 600px;
  margin: auto;          /* centres the panel horizontally */
  padding: 2rem;
  align-items: center;
}

.hex-validator vaadin-text-area {
  width: 100%;           /* text area spans the full panel width */
}
```

**`.loading` — spinner overlay on the button face (Requirement 8.1):**
```css
/* Wrapper gives the button a positioning context */
.check-btn-wrapper {
  position: relative;
}

/* Overlay covers the entire button face with a spinning circle */
.loading::after {
  content: '';
  position: absolute;
  inset: 0;
  margin: auto;
  width: 1.2em;
  height: 1.2em;
  border: 2px solid rgba(255,255,255,0.4);
  border-top-color: white;
  border-radius: 50%;
  animation: btn-spin 0.7s linear infinite;
}
```
The `loading` class is added to `checkButton` at the start of validation and removed once the result is received. The `::after` pseudo-element is absolutely positioned over the button face, hiding the label behind the spinner.

**`.wait-indicator`** — removed. The progress bar below the button has been replaced by the spinner overlay.

### Accessibility

- All interactive elements (`TextArea`, `Button`) have accessible labels via `setAriaLabel()` or `setLabel()`
- Color is not the sole indicator of result — "OK" and "FAIL" text accompanies the green/red styling
- Keyboard navigation works for all interactive elements (Vaadin components are keyboard-accessible by default)
