package com.simon.coursemanagement.service;

import com.simon.coursemanagement.model.Course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the in-memory list of courses.
 *
 * <p>The list itself is private. Other classes receive a read-only copy from
 * {@link #getAllCourses()}, so they cannot accidentally change the manager's
 * internal data.</p>
 */
public class CourseManager {

    private final ArrayList<Course> courses = new ArrayList<>();

    /**
     * Validates and adds a new course.
     *
     * @return the newly created course
     * @throws IllegalArgumentException if the course data is invalid or the
     *                                  code is already in the list
     */
    public Course addCourse(String code, String title, int units) {
        Course course = new Course(code, title, units);

        // The recursive search is also used here to prevent duplicate codes.
        if (searchRecursive(course.getCode(), 0) != null) {
            throw new IllegalArgumentException(
                    "A course with code " + course.getCode() + " already exists.");
        }

        courses.add(course);
        return course;
    }

    /**
     * Validates and replaces an existing course.
     *
     * <p>The caller passes the original {@link Course} object that it wants to
     * update. The original object is found by identity, not by its code, so the
     * user may change the code without losing track of the correct record. A new
     * Course is created first, which means every validation rule runs before the
     * stored list is changed. If validation fails, the original course remains
     * exactly as it was.</p>
     *
     * @return the new course object stored in the list
     * @throws IllegalArgumentException if the original is missing, the new data
     *                                  is invalid, or the new code belongs to a
     *                                  different course
     */
    public Course updateCourse(Course original, String code, String title, int units) {
        if (original == null) {
            throw new IllegalArgumentException("The course to update cannot be null.");
        }

        int originalIndex = indexOfCourse(original);
        if (originalIndex < 0) {
            throw new IllegalArgumentException("The course to update is no longer in the list.");
        }

        // Constructing the Course validates the code, title, and units before
        // any stored value is replaced.
        Course updated = new Course(code, title, units);

        // The edited course is allowed to keep its own code. Only the other
        // records are checked for a duplicate.
        for (int index = 0; index < courses.size(); index++) {
            if (index == originalIndex) {
                continue;
            }
            if (courses.get(index).getCode().equals(updated.getCode())) {
                throw new IllegalArgumentException(
                        "A course with code " + updated.getCode() + " already exists.");
            }
        }

        courses.set(originalIndex, updated);
        return updated;
    }

    /**
     * Removes one exact course object from the list.
     *
     * <p>Identity is used instead of the course code because the table may be
     * sorted or filtered. The UI resolves the selected row to its Course object
     * first, then passes that object here.</p>
     *
     * @return the course that was removed
     * @throws IllegalArgumentException if the course is null or is not currently
     *                                  in the list
     */
    public Course deleteCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("The course to delete cannot be null.");
        }

        int index = indexOfCourse(course);
        if (index < 0) {
            throw new IllegalArgumentException("The course to delete is no longer in the list.");
        }
        return courses.remove(index);
    }

    /**
     * Finds the list position of one exact Course object.
     *
     * <p>Course does not override equals, so {@code ==} is deliberately used.
     * This is important for update and delete: the selected table row must refer
     * to the same object that the manager is holding.</p>
     */
    private int indexOfCourse(Course course) {
        for (int index = 0; index < courses.size(); index++) {
            if (courses.get(index) == course) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns a read-only snapshot of the course list. Returning a copy means
     * callers cannot modify the manager's private ArrayList.
     */
    public List<Course> getAllCourses() {
        return Collections.unmodifiableList(new ArrayList<>(courses));
    }

    /**
     * Searches for a course by code, ignoring case and extra spaces.
     *
     * @return the matching course, or {@code null} when no course matches
     * @throws IllegalArgumentException if the supplied code is badly formatted
     */
    public Course searchCourse(String code) {
        String normalizedCode = Course.normalizeCode(code);
        return searchRecursive(normalizedCode, 0);
    }

    /**
     * Small recursive helper that searches one position at a time.
     *
     * <p>Recursion is used here deliberately to satisfy the assessment
     * requirement for a simple recursive feature.</p>
     */
    private Course searchRecursive(String normalizedCode, int index) {
        // Base case: we have passed the last item, so there is no match.
        if (index >= courses.size()) {
            return null;
        }

        Course current = courses.get(index);

        // Match case: this course has the requested code.
        if (current.getCode().equals(normalizedCode)) {
            return current;
        }

        // Recursive step: try the next position in the list.
        return searchRecursive(normalizedCode, index + 1);
    }

    /**
     * Adds up the credit units with a normal loop (not recursion).
     */
    public int computeTotalUnits() {
        int total = 0;
        for (Course course : courses) {
            total += course.getUnits();
        }
        return total;
    }

    public int getCourseCount() {
        return courses.size();
    }

    /**
     * Replaces the whole list after a successful file load.
     *
     * <p>The caller must already have validated the new list. This method is
     * only called after the complete file has been read successfully, so a
     * failed load never changes the existing courses.</p>
     */
    public void replaceAllCourses(List<Course> newCourses) {
        if (newCourses == null) {
            throw new IllegalArgumentException("The new course list cannot be null.");
        }

        ArrayList<Course> copy = new ArrayList<>(newCourses);
        for (Course course : copy) {
            if (course == null) {
                throw new IllegalArgumentException("The new course list cannot contain null courses.");
            }
        }

        courses.clear();
        courses.addAll(copy);
    }
}
