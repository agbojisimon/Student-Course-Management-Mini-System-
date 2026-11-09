package com.simon.coursemanagement;

import com.simon.coursemanagement.model.Course;
import com.simon.coursemanagement.service.CourseManager;
import com.simon.coursemanagement.storage.CourseFileHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Focused checks for the business logic and file handling.
 *
 * <p>This is a plain Java program rather than a JUnit test so the project stays
 * small and has no test-only dependencies. Run it with
 * {@code java -cp target/test-classes;target/classes com.simon.coursemanagement.VerificationChecks}
 * after {@code mvn test-compile}.</p>
 */
public final class VerificationChecks {

    private static int checks = 0;

    private VerificationChecks() {
    }

    public static void main(String[] args) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("course-management-checks-");
        try {
            checkAddAndList();
            checkNormalization();
            checkDuplicatePrevention();
            checkInvalidInput();
            checkRecursiveSearch();
            checkTotalUnits();
            checkUpdateCourse();
            checkDeleteCourse();
            checkSaveLoadRoundTrip(temporaryDirectory);
            checkUpdateDeletePersistence(temporaryDirectory);
            checkMissingFile(temporaryDirectory);
            checkMalformedFileLeavesCoursesUnchanged(temporaryDirectory);
            checkSnapshotSave(temporaryDirectory);
            checkReadOnlyList();
            checkNoTemporaryFilesLeftBehind(temporaryDirectory);
            System.out.println("ALL " + checks + " CHECKS PASSED");
        } finally {
            deleteDirectory(temporaryDirectory);
        }
    }

    private static void checkAddAndList() {
        CourseManager manager = new CourseManager();
        manager.addCourse("COS 201", "Introduction to Programming", 3);
        manager.addCourse("MTH 201", "Mathematics for Computing", 4);

        check(manager.getCourseCount() == 2, "add and count two courses");
        List<Course> courses = manager.getAllCourses();
        check(courses.size() == 2, "list returns two courses");
        check(courses.get(0).getCode().equals("COS 201"), "first course code kept");
        check(courses.get(1).getTitle().equals("Mathematics for Computing"), "second course title kept");
        check(courses.get(0).getUnits() == 3 && courses.get(1).getUnits() == 4, "units kept");
    }

    private static void checkNormalization() {
        CourseManager manager = new CourseManager();
        Course first = manager.addCourse(" cos201 ", "Introduction to Programming", 3);
        Course second = manager.addCourse(" mth  201 ", "Mathematics for Computing", 3);

        check(first.getCode().equals("COS 201"), "cos201 becomes COS 201");
        check(second.getCode().equals("MTH 201"), "mth  201 becomes MTH 201");
        check(manager.searchCourse("cos201") != null, "search ignores case and spacing");
        check(manager.searchCourse(" COS 201 ") != null, "search trims input");
    }

    private static void checkDuplicatePrevention() {
        CourseManager manager = new CourseManager();
        manager.addCourse("COS 201", "Introduction to Programming", 3);

        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("cos 201", "Duplicate Course", 2),
                "duplicate code is rejected");
        check(manager.getCourseCount() == 1, "duplicate course was not added");
    }

    private static void checkInvalidInput() {
        CourseManager manager = new CourseManager();

        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("   ", "Title", 3), "blank code rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS 201", "   ", 3), "blank title rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS21", "Title", 3), "short code rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS 2011", "Title", 3), "long code rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS-201", "Title", 3), "code with dash rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS 201", "Bad\tTitle", 3), "tab in title rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS 201", "Bad\nTitle", 3), "line break in title rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS 201", "Title", 0), "zero units rejected");
        expectThrows(IllegalArgumentException.class,
                () -> manager.addCourse("COS 201", "Title", -3), "negative units rejected");

        IllegalArgumentException overflow = expectThrows(IllegalArgumentException.class,
                () -> Course.parseUnits("999999999999999999999"),
                "integer overflow in units text rejected");
        check(overflow.getMessage().toLowerCase().contains("too large"),
                "overflow message is clear");
        expectThrows(IllegalArgumentException.class,
                () -> Course.parseUnits("three"), "nonnumeric units rejected");
        expectThrows(IllegalArgumentException.class,
                () -> Course.parseUnits(""), "blank units rejected");
        expectThrows(IllegalArgumentException.class,
                () -> Course.parseUnits("0"), "zero units text rejected");
        expectThrows(IllegalArgumentException.class,
                () -> Course.parseUnits("-2"), "negative units text rejected");
        check(Course.parseUnits(" 4 ") == 4, "valid units text accepted");
    }

    private static void checkRecursiveSearch() {
        CourseManager manager = new CourseManager();
        manager.addCourse("AAA 101", "First Course", 1);
        manager.addCourse("BBB 202", "Middle Course", 2);
        manager.addCourse("CCC 303", "Last Course", 3);

        check(manager.searchCourse("AAA 101") != null, "recursive search finds first");
        check(manager.searchCourse("bbb202") != null, "recursive search finds middle with normalization");
        check(manager.searchCourse("CCC 303") != null, "recursive search finds last");
        check(manager.searchCourse("DDD 404") == null, "recursive search reports missing code");

        CourseManager empty = new CourseManager();
        check(empty.searchCourse("AAA 101") == null, "recursive search handles empty list");
    }

    private static void checkTotalUnits() {
        CourseManager manager = new CourseManager();
        manager.addCourse("COS 201", "Introduction to Programming", 3);
        manager.addCourse("MTH 201", "Mathematics for Computing", 4);
        manager.addCourse("GST 201", "Communication in English", 2);

        check(manager.computeTotalUnits() == 9, "total units computed with a loop");
    }

    private static void checkUpdateCourse() {
        CourseManager manager = new CourseManager();
        Course original = manager.addCourse("COS 201", "Introduction to Programming", 3);
        manager.addCourse("MTH 201", "Mathematics for Computing", 4);

        Course updatedTitle = manager.updateCourse(
                original, "COS 201", "Advanced Programming", 3);
        check(updatedTitle.getTitle().equals("Advanced Programming"),
                "update changes the title");
        check(manager.getCourseCount() == 2, "update does not add a duplicate record");
        check(manager.getAllCourses().get(0) == updatedTitle,
                "update keeps the original list position");
        check(manager.searchCourse("COS 201") == updatedTitle,
                "updated course remains searchable by code");

        Course updatedUnits = manager.updateCourse(
                updatedTitle, " cos201 ", "Advanced Programming", 5);
        check(updatedUnits.getCode().equals("COS 201"),
                "update accepts the same normalized code");
        check(updatedUnits.getUnits() == 5, "update changes the units");
        check(manager.computeTotalUnits() == 9, "update changes the total units");

        Course changedCode = manager.updateCourse(
                updatedUnits, "CSC 301", "Advanced Programming", 5);
        check(changedCode.getCode().equals("CSC 301"), "update changes the course code");
        check(manager.searchCourse("COS 201") == null,
                "old code is gone after a code change");
        check(manager.searchCourse("CSC 301") == changedCode,
                "new code finds the updated course");
        check(manager.getCourseCount() == 2, "code change does not change the count");

        expectThrows(IllegalArgumentException.class,
                () -> manager.updateCourse(changedCode, "MTH 201", "Duplicate Code", 5),
                "update rejects another course's code");
        check(manager.searchCourse("CSC 301") == changedCode,
                "rejected duplicate leaves the original record unchanged");
        check(manager.searchCourse("CSC 301").getTitle().equals("Advanced Programming"),
                "rejected duplicate leaves the original title unchanged");

        expectThrows(IllegalArgumentException.class,
                () -> manager.updateCourse(changedCode, "CSC 301", "   ", 5),
                "update rejects a blank title");
        check(manager.searchCourse("CSC 301") == changedCode,
                "invalid title leaves the original record unchanged");

        expectThrows(IllegalArgumentException.class,
                () -> manager.updateCourse(changedCode, "CSC 301", "Advanced Programming", 0),
                "update rejects zero units");
        check(manager.searchCourse("CSC 301").getUnits() == 5,
                "invalid units leave the original units unchanged");

        Course stale = new Course("GST 201", "Stale Course", 2);
        expectThrows(IllegalArgumentException.class,
                () -> manager.updateCourse(stale, "GST 201", "Stale Course", 2),
                "update rejects a course that is not in the list");
        check(manager.getCourseCount() == 2, "failed update leaves the count unchanged");
    }

    private static void checkDeleteCourse() {
        CourseManager manager = new CourseManager();
        Course first = manager.addCourse("AAA 101", "First Course", 1);
        Course middle = manager.addCourse("BBB 202", "Middle Course", 2);
        Course last = manager.addCourse("CCC 303", "Last Course", 3);

        Course removed = manager.deleteCourse(middle);
        check(removed == middle, "delete removes the exact selected course");
        check(manager.getCourseCount() == 2, "delete changes the course count");
        check(manager.searchCourse("BBB 202") == null, "deleted course is gone");
        check(manager.searchCourse("AAA 101") == first, "delete keeps the first course");
        check(manager.searchCourse("CCC 303") == last, "delete keeps the last course");
        check(manager.computeTotalUnits() == 4, "delete updates the total units");

        expectThrows(IllegalArgumentException.class,
                () -> manager.deleteCourse(middle),
                "delete rejects an already removed course");
        check(manager.getCourseCount() == 2, "failed delete leaves the count unchanged");

        expectThrows(IllegalArgumentException.class,
                () -> manager.deleteCourse(null),
                "delete rejects a null course");
        check(manager.getCourseCount() == 2, "null delete leaves the count unchanged");
    }

    private static void checkSaveLoadRoundTrip(Path directory) throws IOException {
        Path file = directory.resolve("round-trip.txt");
        CourseFileHandler handler = new CourseFileHandler(file);

        CourseManager original = new CourseManager();
        original.addCourse("COS 201", "Introduction to Programming", 3);
        original.addCourse("MTH 201", "Mathematics for Computing", 4);
        original.addCourse("GST 201", "Café Communication", 2);

        handler.saveCourses(original.getAllCourses());

        check(Files.exists(file), "save creates the file");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        check(lines.size() == 3, "save writes one line per course");
        check(lines.get(0).equals("COS 201\tIntroduction to Programming\t3"),
                "save uses code-tab-title-tab-units format");

        List<Course> loaded = handler.loadCourses();
        CourseManager restored = new CourseManager();
        restored.replaceAllCourses(loaded);

        check(restored.getCourseCount() == 3, "load restores all courses");
        check(restored.searchCourse("cos201") != null, "loaded course is searchable");
        check(restored.searchCourse("GST 201").getTitle().equals("Café Communication"),
                "UTF-8 title survives round trip");
        check(restored.computeTotalUnits() == 9, "loaded total units are correct");
    }

    private static void checkUpdateDeletePersistence(Path directory) throws IOException {
        Path file = directory.resolve("updated-deleted.txt");
        CourseFileHandler handler = new CourseFileHandler(file);
        CourseManager manager = new CourseManager();

        manager.addCourse("COS 201", "Introduction to Programming", 3);
        manager.addCourse("MTH 201", "Mathematics for Computing", 4);
        manager.addCourse("GST 201", "Communication in English", 2);

        Course cosCourse = manager.searchCourse("COS 201");
        manager.updateCourse(cosCourse, "CSC 301", "Advanced Programming", 4);

        Course mthCourse = manager.searchCourse("MTH 201");
        manager.deleteCourse(mthCourse);

        handler.saveCourses(manager.getAllCourses());
        List<Course> loaded = handler.loadCourses();

        check(loaded.size() == 2, "save after update/delete writes two records");
        check(loaded.stream().anyMatch(course ->
                        course.getCode().equals("CSC 301")
                                && course.getTitle().equals("Advanced Programming")
                                && course.getUnits() == 4),
                "updated record survives save and reload");
        check(loaded.stream().noneMatch(course -> course.getCode().equals("MTH 201")),
                "deleted record stays absent after save and reload");
        check(loaded.stream().anyMatch(course -> course.getCode().equals("GST 201")),
                "untouched record survives save and reload");

        CourseManager restored = new CourseManager();
        restored.replaceAllCourses(loaded);
        check(restored.computeTotalUnits() == 6,
                "reloaded total units reflect the update and delete");
    }

    private static void checkMissingFile(Path directory) {
        Path file = directory.resolve("does-not-exist.txt");
        CourseFileHandler handler = new CourseFileHandler(file);

        FileNotFoundException error = expectThrows(FileNotFoundException.class,
                handler::loadCourses, "missing file is reported");
        check(error.getMessage().contains("No saved file"), "missing file message is clear");
    }

    private static void checkMalformedFileLeavesCoursesUnchanged(Path directory) throws IOException {
        Path file = directory.resolve("malformed.txt");
        Files.writeString(file,
                "COS 201\tIntroduction to Programming\t3\n"
                        + "MTH 201\tMathematics for Computing\tnot-a-number\n",
                StandardCharsets.UTF_8);

        CourseManager liveManager = new CourseManager();
        liveManager.addCourse("GST 201", "Communication in English", 2);
        CourseFileHandler handler = new CourseFileHandler(file);

        IOException error = expectThrows(IOException.class,
                () -> liveManager.replaceAllCourses(handler.loadCourses()),
                "malformed units row is rejected");
        check(error.getMessage().contains("Line 2"), "malformed row reports its line number");
        check(liveManager.getCourseCount() == 1, "failed load leaves existing count unchanged");
        check(liveManager.searchCourse("GST 201") != null, "failed load leaves existing course unchanged");

        Path duplicateFile = directory.resolve("duplicate.txt");
        Files.writeString(duplicateFile,
                "COS 201\tIntroduction to Programming\t3\n"
                        + "cos 201\tAnother Title\t2\n",
                StandardCharsets.UTF_8);

        IOException duplicateError = expectThrows(IOException.class,
                new CourseFileHandler(duplicateFile)::loadCourses,
                "duplicate code in saved file is rejected");
        check(duplicateError.getMessage().contains("Line 2"), "duplicate row reports its line number");

        Path malformedFieldsFile = directory.resolve("bad-fields.txt");
        Files.writeString(malformedFieldsFile,
                "COS 201\tIntroduction to Programming\t3\n"
                        + "MTH 201\tOnly Two Fields\n",
                StandardCharsets.UTF_8);

        IOException fieldsError = expectThrows(IOException.class,
                new CourseFileHandler(malformedFieldsFile)::loadCourses,
                "malformed field count is rejected");
        check(fieldsError.getMessage().contains("Line 2"), "field-count error reports its line number");
    }

    private static void checkSnapshotSave(Path directory) throws IOException {
        Path file = directory.resolve("snapshot.txt");
        CourseFileHandler handler = new CourseFileHandler(file);
        CourseManager manager = new CourseManager();

        manager.addCourse("COS 201", "Introduction to Programming", 3);
        handler.saveCourses(manager.getAllCourses());

        manager.addCourse("MTH 201", "Mathematics for Computing", 4);
        handler.saveCourses(manager.getAllCourses());

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        check(lines.size() == 2, "second save replaces the snapshot instead of appending");
        check(lines.get(0).startsWith("COS 201\t"), "first snapshot row kept");
        check(lines.get(1).startsWith("MTH 201\t"), "new snapshot row added");

        List<Course> loaded = handler.loadCourses();
        check(loaded.size() == 2, "snapshot reloads exactly two courses");
    }

    private static void checkReadOnlyList() {
        CourseManager manager = new CourseManager();
        manager.addCourse("COS 201", "Introduction to Programming", 3);

        expectThrows(UnsupportedOperationException.class,
                () -> manager.getAllCourses().add(new Course("MTH 201", "Mathematics", 3)),
                "internal list is not exposed as mutable");
    }

    private static void checkNoTemporaryFilesLeftBehind(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            boolean hasTemporaryFile = files
                    .map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.startsWith("courses-") && name.endsWith(".tmp"));
            check(!hasTemporaryFile, "temporary save files are cleaned up");
        }
    }

    private static <T extends Throwable> T expectThrows(
            Class<T> expectedType,
            CheckedAction action,
            String label) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                checks++;
                return expectedType.cast(actual);
            }
            throw new AssertionError(
                    label + ": expected " + expectedType.getSimpleName()
                            + " but got " + actual.getClass().getSimpleName(),
                    actual);
        }
        throw new AssertionError(
                label + ": expected " + expectedType.getSimpleName()
                        + " but no exception was thrown");
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("CHECK FAILED: " + label);
        }
        checks++;
    }

    private static void deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    System.err.println("Could not delete temporary check file "
                            + path + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Could not clean temporary check directory "
                    + directory + ": " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
