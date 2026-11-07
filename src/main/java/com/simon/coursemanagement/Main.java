package com.simon.coursemanagement;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorDeviceConfiguration;
import com.simon.coursemanagement.service.CourseManager;
import com.simon.coursemanagement.storage.CourseFileHandler;
import com.simon.coursemanagement.ui.ConsoleMenu;

import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Application entry point.
 *
 * <p>On Windows, Lanterna is started with its Swing terminal so the text
 * interface opens in a normal desktop window. This avoids terminal/console
 * compatibility problems when the program is launched from IntelliJ.</p>
 */
public class Main {

    public static void main(String[] args) {
        // A desktop is required because this application uses Lanterna's Swing
        // window. This check also gives a clear message in headless CI systems.
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("This application needs a desktop environment to show the Lanterna window.");
            return;
        }

        SwingTerminalFrame terminal = null;
        TerminalScreen screen = null;

        try {
            // SwingTerminalFrame is Lanterna's ready-made Swing window. It is
            // also a Terminal, so TerminalScreen can read keys and draw output.
            terminal = new SwingTerminalFrame(
                    "COS 201 - Student Course Management",
                    new TerminalSize(110, 34),
                    TerminalEmulatorDeviceConfiguration.getDefault(),
                    SwingTerminalFontConfiguration.getDefaultOfSize(16),
                    TerminalEmulatorColorConfiguration.getDefault(),
                    TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode);
            terminal.setLocationRelativeTo(null);

            screen = new TerminalScreen(terminal);
            screen.startScreen();

            // SwingTerminalFrame starts hidden, so show it explicitly after the
            // screen is ready. This line is required for the UI to appear.
            terminal.setVisible(true);

            // MultiWindowTextGUI draws the windows and handles keyboard focus.
            // The EmptySpace sets the area behind the window to white.
            MultiWindowTextGUI gui = new MultiWindowTextGUI(
                    screen,
                    new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.WHITE));
            gui.setTheme(ConsoleMenu.createTheme());

            CourseManager courseManager = new CourseManager();
            CourseFileHandler fileHandler = new CourseFileHandler();
            ConsoleMenu menu = new ConsoleMenu(gui, courseManager, fileHandler);
            BasicWindow window = menu.createWindow();

            // Make the Swing window's X button use the same safe-exit check as
            // the Exit Program button, including the unsaved-changes prompt.
            terminal.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            terminal.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    gui.getGUIThread().invokeLater(menu::requestExit);
                }
            });

            // This blocks until the menu window is closed.
            gui.addWindowAndWait(window);
        } catch (IOException e) {
            System.err.println("Could not start the application: " + e.getMessage());
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.err.println("Unexpected error while starting the application: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up the terminal even after an error. Problems here are
            // reported rather than silently ignored.
            if (screen != null) {
                try {
                    screen.stopScreen();
                } catch (IOException e) {
                    System.err.println("Could not stop the screen cleanly: " + e.getMessage());
                }
            }
            if (terminal != null) {
                terminal.close();
            }
        }
    }
}
