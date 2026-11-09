package com.simon.coursemanagement.ui;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Button with custom colours for normal, hover, pressed, focus, and disabled states. */
public final class AppButton extends JButton {

    private final Color normalBackground;
    private final Color hoverBackground;
    private final Color pressedBackground;
    private final Color normalForeground;
    private final Color borderColor;
    private final Color focusColor;
    private final int arc;

    private boolean active;

    private AppButton(String text,
                      Color normalBackground,
                      Color hoverBackground,
                      Color pressedBackground,
                      Color normalForeground,
                      Color borderColor,
                      Color focusColor,
                      boolean sidebarButton,
                      int arc) {
        super(text);
        this.normalBackground = normalBackground;
        this.hoverBackground = hoverBackground;
        this.pressedBackground = pressedBackground;
        this.normalForeground = normalForeground;
        this.borderColor = borderColor;
        this.focusColor = focusColor;
        this.arc = arc;

        setFont(UiTheme.font(14, Font.BOLD));
        setBorder(new EmptyBorder(9, 18, 9, 18));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (sidebarButton) {
            setHorizontalAlignment(SwingConstants.LEFT);
        }
    }

    /** Creates a blue call-to-action button with white text. */
    public static AppButton primary(String text) {
        return new AppButton(text, UiTheme.BLUE, UiTheme.BLUE_HOVER, UiTheme.BLUE_PRESSED,
                UiTheme.WHITE, null, UiTheme.BLUE_FOCUS, false, 12);
    }

    /** Creates a white button with a visible grey border. */
    public static AppButton secondary(String text) {
        return new AppButton(text, UiTheme.WHITE, UiTheme.SURFACE,
                new Color(241, 245, 249), UiTheme.TEXT, UiTheme.BORDER_STRONG,
                UiTheme.BLUE, false, 12);
    }

    /** Creates a dark sidebar button that can be marked active. */
    public static AppButton sidebar(String text) {
        return new AppButton(text, new Color(0, 0, 0, 0), UiTheme.NAVY_HOVER,
                UiTheme.NAVY_PRESSED, UiTheme.NAVY_TEXT, null,
                UiTheme.BLUE_FOCUS, true, 10);
    }

    /** Marks the active sidebar button. */
    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    public Color getForeground() {
        if (!isEnabled()) {
            return UiTheme.DISABLED_TEXT;
        }
        if (active) {
            return UiTheme.WHITE;
        }
        return normalForeground;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        Color fill = chooseBackground();

        if (fill.getAlpha() > 0) {
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc);
        }

        if (borderColor != null && isEnabled()) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
        }
        g2.dispose();

        // Draw the background first, then let Swing paint the text.
        super.paintComponent(graphics);

        if (isFocusOwner() && isEnabled()) {
            Graphics2D focus = (Graphics2D) graphics.create();
            focus.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            focus.setColor(focusColor);
            focus.setStroke(new BasicStroke(2f));
            focus.drawRoundRect(2, 2, width - 5, height - 5, arc, arc);
            focus.dispose();
        }
    }

    private Color chooseBackground() {
        if (!isEnabled()) {
            return UiTheme.DISABLED_BACKGROUND;
        }
        if (active) {
            if (getModel().isPressed()) {
                return UiTheme.BLUE_PRESSED;
            }
            if (getModel().isRollover()) {
                return UiTheme.BLUE_HOVER;
            }
            return UiTheme.BLUE;
        }
        if (getModel().isPressed()) {
            return pressedBackground;
        }
        if (getModel().isRollover()) {
            return hoverBackground;
        }
        return normalBackground;
    }
}
