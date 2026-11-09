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

/** Saves and loads courses in a UTF-8 text file, one course per line. */
public class CourseFileHandler {

    private static final String DEFAULT_DIRECTORY = "data";
    private static final String DEFAULT_FILE_NAME = "courses.txt";

    private final Path filePath;

    /** Uses data/courses.txt. */
    public CourseFileHandler() {
        this(Path.of(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME));
    }

    /** Uses a custom file path, mainly for tests. */
    public CourseFileHandler(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null.");
        }
        this.filePath = filePath;
    }

    public Path getFilePath() {
        return filePath;
    }

    /** Returns the path shown in user messages. */
    public String getDisplayPath() {
        return filePath.toString();
    }

    /**
     * Writes the whole list to disk as a snapshot.
     * A temporary file protects the old file if saving fails.
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

            // Replace the old file only after the new one is complete.
            try {
                Files.move(temporaryFile, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Remove the temp file if it still exists.
            Files.deleteIfExists(temporaryFile);
        }
    }

    /** Reads and validates the whole file before returning any courses. */
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
            // The file may disappear between the check and the read.
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
                // Use the same unit rules as manual entry.
                units = Course.parseUnits(unitsText);
            } catch (IllegalArgumentException e) {
                throw new IOException("Line " + lineNumber + ": " + e.getMessage());
            }

            try {
                // This also validates the code and title and rejects duplicates.
                parsedCourses.addCourse(code, title, units);
            } catch (IllegalArgumentException e) {
                throw new IOException("Line " + lineNumber + ": " + e.getMessage());
            }
        }

        return parsedCourses.getAllCourses();
    }

    /** Rejects characters that would break the save format. */
    private static boolean containsTabOrLineBreak(String value) {
        return value.indexOf('\t') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
    }
}
