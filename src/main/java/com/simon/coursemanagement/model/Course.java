package com.simon.coursemanagement.model;

import java.util.Locale;

/** One course record. Values are validated when the object is created. */
public final class Course {

    private final String code;
    private final String title;
    private final int units;

    /** Creates a course and validates its values. */
    public Course(String code, String title, int units) {
        this.code = normalizeCode(code);
        this.title = normalizeTitle(title);
        if (units <= 0) {
            throw new IllegalArgumentException("Credit units must be a positive whole number.");
        }
        this.units = units;
    }

    /** Normalizes a code to the project format, for example "cos201" -> "COS 201". */
    public static String normalizeCode(String rawCode) {
        if (rawCode == null) {
            throw new IllegalArgumentException("Course code is required.");
        }

        String trimmed = rawCode.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Course code is required.");
        }
        if (containsTabOrLineBreak(trimmed)) {
            throw new IllegalArgumentException("Course code must not contain tabs or line breaks.");
        }

        // Ignore spaces and letter case before checking the format.
        String compact = trimmed.replace(" ", "").toUpperCase(Locale.ROOT);
        if (!compact.matches("[A-Z]{3}[0-9]{3}")) {
            throw new IllegalArgumentException(
                    "Course code must be three letters followed by three digits (for example, COS 201).");
        }

        // Store the readable form with one space.
        return compact.substring(0, 3) + " " + compact.substring(3);
    }

    /** Trims a title and rejects tabs or line breaks. */
    public static String normalizeTitle(String rawTitle) {
        if (rawTitle == null) {
            throw new IllegalArgumentException("Course title is required.");
        }

        String trimmed = rawTitle.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Course title is required.");
        }
        if (containsTabOrLineBreak(trimmed)) {
            throw new IllegalArgumentException("Course title must not contain tabs or line breaks.");
        }
        return trimmed;
    }

    /** Parses a positive whole number of credit units. */
    public static int parseUnits(String rawUnits) {
        String text = rawUnits == null ? "" : rawUnits.trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException("Credit units is required.");
        }
        if (!text.matches("[0-9]+")) {
            throw new IllegalArgumentException(
                    "Credit units must be a positive whole number (for example, 3).");
        }

        try {
            int units = Integer.parseInt(text);
            if (units <= 0) {
                throw new IllegalArgumentException("Credit units must be greater than 0.");
            }
            return units;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Credit units is too large. Enter a value no greater than 2,147,483,647.");
        }
    }

    /** Rejects characters that would break the save file format. */
    private static boolean containsTabOrLineBreak(String value) {
        return value.indexOf('\t') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public int getUnits() {
        return units;
    }

    /** Readable text for status messages. */
    @Override
    public String toString() {
        return code + " - " + title + " (" + units + (units == 1 ? " unit)" : " units)");
    }
}
