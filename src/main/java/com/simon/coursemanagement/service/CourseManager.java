package com.simon.coursemanagement.service;

import com.simon.coursemanagement.model.Course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps the in-memory course list and enforces the main rules. */
public class CourseManager {

    private final ArrayList<Course> courses = new ArrayList<>();

    /** Adds a new course after validating it and checking for duplicates. */
    public Course addCourse(String code, String title, int units) {
        Course course = new Course(code, title, units);

        // Duplicate codes are rejected with the same recursive search.
        if (searchRecursive(course.getCode(), 0) != null) {
            throw new IllegalArgumentException(
                    "A course with code " + course.getCode() + " already exists.");
        }

        courses.add(course);
        return course;
    }

    /** Replaces a course by object identity, so its code can change. */
    public Course updateCourse(Course original, String code, String title, int units) {
        if (original == null) {
            throw new IllegalArgumentException("The course to update cannot be null.");
        }

        int originalIndex = indexOfCourse(original);
        if (originalIndex < 0) {
            throw new IllegalArgumentException("The course to update is no longer in the list.");
        }

        // Validate the new values before changing the list.
        Course updated = new Course(code, title, units);

        // The edited course may keep its own code; only other records are checked.
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

    /** Removes the exact course object selected in the table. */
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

    /** Finds a course by object identity. */
    private int indexOfCourse(Course course) {
        for (int index = 0; index < courses.size(); index++) {
            if (courses.get(index) == course) {
                return index;
            }
        }
        return -1;
    }

    /** Returns a read-only copy of the course list. */
    public List<Course> getAllCourses() {
        return Collections.unmodifiableList(new ArrayList<>(courses));
    }

    /** Finds a course by code, ignoring case and extra spaces. */
    public Course searchCourse(String code) {
        String normalizedCode = Course.normalizeCode(code);
        return searchRecursive(normalizedCode, 0);
    }

    /** Searches one list position per call. */
    private Course searchRecursive(String normalizedCode, int index) {
        // Base case: no more courses.
        if (index >= courses.size()) {
            return null;
        }

        Course current = courses.get(index);

        // Match found.
        if (current.getCode().equals(normalizedCode)) {
            return current;
        }

        // Try the next course.
        return searchRecursive(normalizedCode, index + 1);
    }

    /** Adds the credit units with a normal loop. */
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

    /** Replaces the list after a successful file load. */
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
