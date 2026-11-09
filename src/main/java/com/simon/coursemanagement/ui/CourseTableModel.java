package com.simon.coursemanagement.ui;

import com.simon.coursemanagement.model.Course;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model that exposes courses to the Swing {@code JTable}.
 *
 * <p>The model receives a snapshot of courses and never modifies the service
 * layer. This keeps the table display separate from the business rules.</p>
 */
public final class CourseTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
            "Course Code", "Course Title", "Credit Units"
    };

    private List<Course> courses = new ArrayList<>();

    public void setCourses(List<Course> courses) {
        this.courses = new ArrayList<>(courses);
        fireTableDataChanged();
    }

    public Course getCourseAt(int rowIndex) {
        return courses.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return courses.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Course course = courses.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> course.getCode();
            case 1 -> course.getTitle();
            case 2 -> course.getUnits();
            default -> "";
        };
    }
}
