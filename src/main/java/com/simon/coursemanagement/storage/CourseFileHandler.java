package com.simon.coursemanagement.storage;

import com.simon.coursemanagement.model.Course;
import com.simon.coursemanagement.service.CourseManager;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Saves courses to a UTF-8 text file and loads them back.
 *
 * <p>File format: one course per line, with exactly three tab-separated
 * fields:</p>
 *
 * <pre>CODE&lt;TAB&gt;TITLE&lt;TAB&gt;UNITS</pre>
 *
 * <p>The default file is {@code data/courses.txt}, relative to the project
 * root. The data directory is created automatically on the first save.</p>
 */
public class CourseFileHandler {

    private static final String DEFAULT_DIRECTORY = "data";
    private static final String DEFAULT_FILE_NAME = "courses.txt";

    private final Path filePath;

    /**
     * Uses the normal save location: data/courses.txt.
     */
    public CourseFileHandler() {
        this(Path.of(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME));
    }

    /**
     * Allows a different path, which is useful for tests and temporary files.
     */
    public CourseFileHandler(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null.");
        }
        this.filePath = filePath;
    }

    public Path getFilePath() {
        return filePath;
    }

    /**
     * Path shown in user messages. It stays relative when the handler was
     * created with a relative path, so messages are easier to read.
     */
    public String getDisplayPath() {
        return filePath.toString();
    }

    /**
     * Saves the whole list as one snapshot. The file is not appended to, so
     * saving twice cannot create duplicate records.
     *
     * <p>A temporary file is written first and then moved over the real file.
     * If writing fails, the previous saved file is left in place.</p>
     *
     * @throws IOException if the directory or file cannot be written
     */
    public void saveCourses(List<Course> courses) throws IOException {
        if (courses == null) {
            throw new IllegalArgumentException("Course list cannot be null.");
        }

        Path target = filePath.toAbsolutePath();
        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Cannot work out a folder for " + getDisplayPath() + ".");
        }

        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, "courses-", ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                for (Course course : courses) {
                    if (course == null) {
                        throw new IOException("Cannot save an empty course record.");
                    }
                    if (containsTabOrLineBreak(course.getCode())
                            || containsTabOrLineBreak(course.getTitle())) {
                        throw new IOException("A course contains a tab or line break and cannot be saved.");
                    }

                    writer.write(course.getCode());
                    writer.write('\t');
                    writer.write(course.getTitle());
                    writer.write('\t');
                    writer.write(Integer.toString(course.getUnits()));
                    writer.newLine();
                }
            }

            // Replace the old file only after the temporary file is complete.
            try {
                Files.move(temporaryFile, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // If the move succeeded this file no longer exists, so this is a
            // no-op. If writing failed, it removes the incomplete temp file.
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Reads and validates an entire file before returning any courses.
     *
     * <p>A temporary CourseManager is used while parsing. If any line is
     * malformed, has invalid units, or repeats a code, this method throws an
     * IOException with the line number. The caller's current list is therefore
     * never changed by a failed load.</p>
     *
     * @throws IOException if the file is missing, unreadable, or contains bad data
     */
    public List<Course> loadCourses() throws IOException {
        Path source = filePath.toAbsolutePath();

        if (!Files.exists(source)) {
            throw new FileNotFoundException(
                    "No saved file was found at " + getDisplayPath() + ".");
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            // Handles the rare case where the file disappears between the
            // exists() check and the read.
            throw new FileNotFoundException(
                    "No saved file was found at " + getDisplayPath() + ".");
        } catch (IOException e) {
            throw new IOException(
                    "Could not read " + getDisplayPath() + ": " + e.getMessage(), e);
        }

        CourseManager parsedCourses = new CourseManager();
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            String[] fields = line.split("\t", -1);

            if (fields.length != 3) {
                throw new IOException("Line " + lineNumber
                        + ": expected 3 tab-separated fields (code, title, units) but found "
                        + fields.length + ".");
            }

            String code = fields[0].trim();
            String title = fields[1].trim();
            String unitsText = fields[2].trim();

            int units;
            try {
                // Use the same unit rules as the user interface so a file
                // cannot accept text that manual entry would reject.
                units = Course.parseUnits(unitsText);
            } catch (IllegalArgumentException e) {
                throw new IOException("Line " + lineNumber + ": " + e.getMessage());
            }

            try {
                // This also validates the code/title and rejects duplicates.
                parsedCourses.addCourse(code, title, units);
            } catch (IllegalArgumentException e) {
                throw new IOException("Line " + lineNumber + ": " + e.getMessage());
            }
        }

        return parsedCourses.getAllCourses();
    }

    /**
     * Defensive check: the save format cannot store these characters.
     */
    private static boolean containsTabOrLineBreak(String value) {
        return value.indexOf('\t') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
    }
}
