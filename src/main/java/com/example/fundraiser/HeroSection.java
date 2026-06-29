package com.example.fundraiser;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;

/**
 * Hero section component displayed at the top of the fundraiser page.
 * Spans the full viewport width and contains a hero image.
 */
public class HeroSection extends Div {

    public HeroSection() {
        setWidthFull();
        addClassName("hero-section");

        Image heroImage = new Image("images/hero.jpg", "Fundraiser hero image");
        heroImage.setWidthFull();
        add(heroImage);
    }
}
