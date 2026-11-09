package com.simon.coursemanagement.model;

import java.util.Locale;

/**
 * Represents one course in the student's course list.
 *
 * <p>A course stores a normalized course code, a title, and a positive number
 * of credit units. Validation happens in the constructor so that an invalid
 * Course object can never be created.</p>
 */
public final class Course {

    private final String code;
    private final String title;
    private final int units;

    /**
     * Creates a validated course.
     *
     * @param code  the course code entered by the user, for example " cos201 "
     * @param title the course title
     * @param units the number of credit units, which must be greater than zero
     * @throws IllegalArgumentException if any value is invalid
     */
    public Course(String code, String title, int units) {
        this.code = normalizeCode(code);
        this.title = normalizeTitle(title);
        if (units <= 0) {
            throw new IllegalArgumentException("Credit units must be a positive whole number.");
        }
        this.units = units;
    }

    /**
     * Normalizes and validates a course code.
     *
     * <p>Project assumption: this mini-system accepts a three-letter subject
     * prefix followed by three digits. This is a rule chosen for this project,
     * not a university-wide rule. Spaces are ignored so that "cos201",
     * "COS 201", and " cos  201 " all become "COS 201".</p>
     *
     * @param rawCode the text entered by the user
     * @return the code in the form "ABC 123"
     * @throws IllegalArgumentException if the code is blank or badly formatted
     */
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

        // Removing spaces makes the comparison independent of how the user
        // spaced the code. Locale.ROOT avoids locale-specific case rules.
        String compact = trimmed.replace(" ", "").toUpperCase(Locale.ROOT);
        if (!compact.matches("[A-Z]{3}[0-9]{3}")) {
            throw new IllegalArgumentException(
                    "Course code must be three letters followed by three digits (for example, COS 201).");
        }

        // Store the readable form with one space between the prefix and digits.
        return compact.substring(0, 3) + " " + compact.substring(3);
    }

    /**
     * Trims and validates a course title.
     *
     * <p>Tabs and line breaks are rejected because the save file uses tabs to
     * separate fields and new lines to separate records. The method is public
     * so the Swing form can check the title field before submitting the whole
     * course to {@link com.simon.coursemanagement.service.CourseManager}.</p>
     */
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

    /**
     * Converts text entered for credit units into a positive int.
     *
     * <p>This method is kept in one place so the user interface uses the same
     * rules that can be checked directly in the verification program.</p>
     *
     * @throws IllegalArgumentException for blank, nonnumeric, zero, negative,
     *                                  or too-large values
     */
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

    /**
     * Checks for characters that would break the tab-separated storage format.
     */
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

    /**
     * A readable form used in status messages and in the console for debugging.
     */
    @Override
    public String toString() {
        return code + " - " + title + " (" + units + (units == 1 ? " unit)" : " units)");
    }
}
