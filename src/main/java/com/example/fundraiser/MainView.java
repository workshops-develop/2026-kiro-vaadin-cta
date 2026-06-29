package com.example.fundraiser;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Root view of the fundraiser single-page application.
 *
 * <p>Satisfies Requirements 1.3, 2.1, 3.1, 4.1, 4.2, 4.3:
 * <ul>
 *   <li>1.3 – operates as a single-page application via {@code @Route("")}</li>
 *   <li>2.1 – displays {@link HeroSection} at the top of the page</li>
 *   <li>3.1 – displays multiple {@link TextBlock} instances below the hero</li>
 *   <li>4.1 – applies visual styling consistent with a fundraiser call-to-action page</li>
 *   <li>4.2 – uses design elements that encourage user engagement</li>
 *   <li>4.3 – presents content in a visually appealing manner</li>
 * </ul>
 */
@Route("")
@PageTitle("Fundraiser")
public class MainView extends VerticalLayout {

    public MainView(ValidationService validationService) {
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        // Requirement 2.1: Hero section at the top of the page
        add(new HeroSection());

        // Requirement 3.1: Multiple text blocks with fundraiser content
        add(new TextBlock(
                "<h2>Help Us Make a Difference</h2>" +
                "<p>Every contribution brings us one step closer to our goal. " +
                "Our mission is to provide vital resources to communities in need — " +
                "clean water, education, and healthcare for those who need it most. " +
                "Together, we can create lasting change that transforms lives.</p>"
        ));

        add(new TextBlock(
                "<h3>Why Your Support Matters</h3>" +
                "<p>Last year, thanks to generous donors like you, we reached over 10,000 " +
                "families across three continents. With your help today, we can double that " +
                "impact. No contribution is too small — every dollar goes directly to the " +
                "people who need it.</p>"
        ));

        add(new TextBlock(
                "<h3>Validate Your Contribution Key</h3>" +
                "<p>If you have received a unique contribution key, please enter it below " +
                "to verify and activate your donation. Your generosity is what makes our " +
                "work possible — thank you for being part of this journey.</p>"
        ));

        // Hex validator component for key validation
        add(new HexValidatorComponent(validationService));
    }
}
