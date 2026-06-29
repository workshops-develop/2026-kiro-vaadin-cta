package com.example.fundraiser;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

/**
 * Vaadin application shell configurator.
 *
 * In Vaadin 24, @Theme must be placed on an AppShellConfigurator class,
 * not on a @Route view.
 *
 * @Push enables server-initiated UI updates so that ui.access() calls from
 * background threads are pushed to the browser immediately, without waiting
 * for the next user interaction. This is required for the wait animation to
 * stop as soon as the validation response (or timeout) is received.
 */
@Push
@Theme("fundraiser")
public class AppShell implements AppShellConfigurator {
}
