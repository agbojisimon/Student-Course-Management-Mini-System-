package com.simon.coursemanagement;

import com.formdev.flatlaf.FlatLightLaf;
import com.simon.coursemanagement.service.CourseManager;
import com.simon.coursemanagement.storage.CourseFileHandler;
import com.simon.coursemanagement.ui.MainFrame;
import com.simon.coursemanagement.ui.UiTheme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Application entry point.
 *
 * <p>Swing components must be created on the Event Dispatch Thread, which is
 * why the frame construction is wrapped in {@link SwingUtilities#invokeLater}.
 * FlatLaf is installed before the frame is built so every control uses the
 * same modern look and feel.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("This application needs a desktop environment to show the Swing window.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                configureGlobalStyles();

                MainFrame frame = new MainFrame(new CourseManager(), new CourseFileHandler());
                frame.setVisible(true);
            } catch (RuntimeException error) {
                error.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Could not start the application: " + error.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Applies the shared blue focus treatment and readable selection colours.
     * These values sit alongside the explicit fonts and colours in MainFrame.
     */
    private static void configureGlobalStyles() {
        UIManager.put("defaultFont", UiTheme.font(14, Font.PLAIN));

        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.focusColor", UiTheme.BLUE);
        UIManager.put("Component.borderColor", UiTheme.BORDER_STRONG);
        UIManager.put("Component.focusedBorderColor", UiTheme.BLUE);
        UIManager.put("Component.disabledBorderColor", UiTheme.BORDER);

        UIManager.put("TextField.selectionBackground", UiTheme.BLUE_SOFT);
        UIManager.put("TextField.selectionForeground", UiTheme.TEXT);

        UIManager.put("Table.selectionBackground", UiTheme.BLUE);
        UIManager.put("Table.selectionForeground", UiTheme.WHITE);
        UIManager.put("TableHeader.background", UiTheme.SURFACE);
        UIManager.put("TableHeader.foreground", UiTheme.MUTED);

        UIManager.put("OptionPane.background", UiTheme.WHITE);
        UIManager.put("OptionPane.messageForeground", UiTheme.TEXT);
    }
}
