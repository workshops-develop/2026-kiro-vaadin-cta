package com.example.fundraiser;

import com.vaadin.flow.component.html.Div;

/**
 * A reusable full-width content section that renders HTML content.
 * Satisfies Requirements 3.1, 3.2, 3.3, 3.4.
 */
public class TextBlock extends Div {

    public TextBlock(String htmlContent) {
        setWidthFull();
        addClassName("text-block");
        getElement().setProperty("innerHTML", htmlContent);
    }
}
