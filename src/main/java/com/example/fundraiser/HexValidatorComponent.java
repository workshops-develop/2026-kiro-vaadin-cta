package com.example.fundraiser;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

/**
 * Interactive component for hexadecimal key input and validation.
 *
 * <p>Satisfies Requirements 5.1–5.5, 6.1–6.3, 7.1–7.4, 8.1, 8.3–8.5, 10.1, 10.2, 10.5.
 */
public class HexValidatorComponent extends VerticalLayout {

    private final TextArea keyInput;
    private final Button randomGeneratorButton;
    private final Button checkButton;
    private final Span resultDisplay;
    private final ValidationService validationService;

    public HexValidatorComponent(ValidationService validationService) {
        this.validationService = validationService;

        // Centre all children and span full width
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        addClassName("hex-validator");

        // --- Key Input (Requirement 5.1, 5.2, 5.3, 5.4, 5.5) ---
        keyInput = new TextArea();
        keyInput.setWidthFull();
        keyInput.setMaxLength(240);
        keyInput.setLabel("Hex Key");
        keyInput.setAriaLabel("Hexadecimal key input");
        keyInput.getElement().setAttribute("pattern", "[0-9a-fA-F]*");
        keyInput.addValueChangeListener(event -> {
            String filtered = HexInputValidator.stripNonHex(event.getValue());
            if (!filtered.equals(event.getValue())) {
                keyInput.setValue(filtered);
            }
            keyInput.setInvalid(false);
            keyInput.setErrorMessage(null);
        });

        // --- Random Generator Button (Requirement 6.1, 6.2, 6.3) ---
        randomGeneratorButton = new Button("Generate Random Key");
        randomGeneratorButton.addClickListener(event ->
                keyInput.setValue(HexStringGenerator.generate()));

        // --- Check Button (Requirement 7.1, 8.1) ---
        // check-btn-wrapper gives the button a positioning context for the CSS overlay spinner
        checkButton = new Button("Check Key");
        checkButton.setAriaLabel("Validate hex key");
        checkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        checkButton.addClassName("check-btn-wrapper");
        checkButton.addClickListener(event -> handleCheckButtonClick());

        // --- Result Display — shown next to the button (Requirement 10.5) ---
        resultDisplay = new Span();
        resultDisplay.addClassName("result-display");
        resultDisplay.setVisible(false);

        // Button and result sit side by side in a horizontal row
        HorizontalLayout buttonRow = new HorizontalLayout(checkButton, resultDisplay);
        buttonRow.setAlignItems(Alignment.CENTER);
        buttonRow.setSpacing(true);

        add(keyInput, randomGeneratorButton, buttonRow);
    }

    /**
     * Handles the Check Button click event.
     *
     * <p>Validates input, shows a CSS overlay spinner on the button, calls the
     * validation service on a background thread, then pushes the result back to
     * the browser via {@code ui.access()} (requires {@code @Push} on AppShell).
     */
    private void handleCheckButtonClick() {
        String key = keyInput.getValue();

        // Requirement 7.3: empty input → inline error, no API call
        if (key == null || key.isEmpty()) {
            keyInput.setErrorMessage("Please enter a hexadecimal key.");
            keyInput.setInvalid(true);
            return;
        }

        // Requirement 7.4: invalid format → inline error, no API call
        if (!HexInputValidator.isValid(key)) {
            keyInput.setErrorMessage("Key must be exactly 240 hexadecimal characters (0-9, a-f, A-F).");
            keyInput.setInvalid(true);
            return;
        }

        // Requirement 8.1, 8.3: disable button and show overlay spinner
        checkButton.setEnabled(false);
        checkButton.addClassName("loading");

        // Hide any previous result while the new request is in flight
        resultDisplay.setVisible(false);

        UI ui = UI.getCurrent();

        Thread validationThread = new Thread(() -> {
            ValidationResult result = validationService.validate(key);

            // Requirement 8.4, 8.5, 10.1, 10.2, 10.5: push UI update to browser
            ui.access(() -> {
                resultDisplay.getElement().getClassList().remove("result-ok");
                resultDisplay.getElement().getClassList().remove("result-fail");

                if (result == ValidationResult.OK) {
                    resultDisplay.setText("OK");
                    resultDisplay.addClassName("result-ok");
                } else {
                    resultDisplay.setText("FAIL");
                    resultDisplay.addClassName("result-fail");
                }

                resultDisplay.setVisible(true);
                checkButton.setEnabled(true);
                checkButton.removeClassName("loading");
            });
        });
        validationThread.setDaemon(true);
        validationThread.start();
    }
}
