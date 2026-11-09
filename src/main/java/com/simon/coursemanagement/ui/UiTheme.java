package com.simon.coursemanagement.ui;

import java.awt.Color;
import java.awt.Font;

/** Shared colours and fonts for the interface. */
public final class UiTheme {

    public static final Color NAVY = new Color(15, 23, 42);
    public static final Color NAVY_HOVER = new Color(30, 41, 59);
    public static final Color NAVY_PRESSED = new Color(51, 65, 85);
    public static final Color NAVY_TEXT = new Color(203, 213, 225);
    public static final Color NAVY_MUTED = new Color(148, 163, 184);

    public static final Color BLUE = new Color(37, 99, 235);
    public static final Color BLUE_HOVER = new Color(29, 78, 216);
    public static final Color BLUE_PRESSED = new Color(30, 64, 175);
    public static final Color BLUE_SOFT = new Color(219, 234, 254);
    public static final Color BLUE_FOCUS = new Color(147, 197, 253);

    public static final Color WHITE = Color.WHITE;
    public static final Color SURFACE = new Color(248, 250, 252);
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color BORDER_STRONG = new Color(203, 213, 225);

    public static final Color SUCCESS = new Color(21, 128, 61);
    public static final Color ERROR = new Color(185, 28, 28);
    public static final Color DISABLED_BACKGROUND = new Color(226, 232, 240);
    public static final Color DISABLED_TEXT = new Color(148, 163, 184);

    private static final String FONT_FAMILY = "Segoe UI";

    private UiTheme() {
    }

    public static Font font(int size, int style) {
        return new Font(FONT_FAMILY, style, size);
    }
}
