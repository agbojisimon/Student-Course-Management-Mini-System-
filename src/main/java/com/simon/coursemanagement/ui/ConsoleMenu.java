package com.simon.coursemanagement.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.AbstractBorder;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Border;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.gui2.table.TableModel;
import com.simon.coursemanagement.model.Course;
import com.simon.coursemanagement.service.CourseManager;
import com.simon.coursemanagement.storage.CourseFileHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Builds and controls the Lanterna text-based user interface.
 *
 * <p>All seven required actions are available as clearly labelled buttons.
 * The menu is event-driven: each button has a method that performs one action,
 * and the interface is never reopened recursively.</p>
 */
public class ConsoleMenu {

    private final WindowBasedTextGUI gui;
    private final CourseManager courseManager;
    private final CourseFileHandler fileHandler;

    private final TextBox codeBox = new TextBox(new TerminalSize(20, 1));
    private final TextBox titleBox = new TextBox(new TerminalSize(52, 1));
    private final TextBox unitsBox = new TextBox(new TerminalSize(8, 1));
    private final Table<String> courseTable = new Table<>("Code", "Title", "Units");
    private final Label summaryLabel = new Label("");
    private final Label statusLabel = new Label("Ready.");
    private final Label tableHintLabel = new Label("");

    private BasicWindow window;
    private boolean unsavedChanges;

    public ConsoleMenu(WindowBasedTextGUI gui,
                       CourseManager courseManager,
                       CourseFileHandler fileHandler) {
        if (gui == null || courseManager == null || fileHandler == null) {
            throw new IllegalArgumentException("ConsoleMenu dependencies cannot be null.");
        }
        this.gui = gui;
        this.courseManager = courseManager;
        this.fileHandler = fileHandler;
    }

    /**
     * Creates the single main window and returns it to Main.
     */
    public BasicWindow createWindow() {
        window = new BasicWindow("Student Course Management");
        window.setHints(Arrays.asList(Window.Hint.FULL_SCREEN));

        Panel mainPanel = new Panel(new LinearLayout(Direction.VERTICAL).setSpacing(1));
        mainPanel.setFillColorOverride(TextColor.ANSI.WHITE);

        Label titleLabel = new Label("Student Course Management System");
        titleLabel.addStyle(SGR.BOLD);
        mainPanel.addComponent(titleLabel);
        mainPanel.addComponent(new Label("COS 201 Programming I - Lab Assessment (Mini-System)"));

        mainPanel.addComponent(titledBorder(" Course Entry ", createEntryPanel()));

        courseTable.setVisibleRows(8);
        courseTable.setPreferredSize(new TerminalSize(100, 10));
        mainPanel.addComponent(titledBorder(" Recorded Courses ", courseTable));
        mainPanel.addComponent(tableHintLabel);

        mainPanel.addComponent(createActionButtons());

        Panel statusPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        summaryLabel.addStyle(SGR.BOLD);
        statusPanel.addComponent(summaryLabel);
        statusPanel.addComponent(statusLabel);
        mainPanel.addComponent(titledBorder(" Status ", statusPanel));

        window.setComponent(mainPanel);
        refreshView();
        codeBox.takeFocus();
        return window;
    }

    /**
     * Creates the custom Lanterna theme.
     *
     * <p>This is more than just changing the outer window: labels, tables,
     * text boxes, buttons, and dialogs all inherit the same white background,
     * black text, and blue button styling. The focused states use a stronger
     * blue so keyboard focus is easy to see.</p>
     */
    public static Theme createTheme() {
        TextColor black = TextColor.ANSI.BLACK;
        TextColor white = TextColor.ANSI.WHITE;

        // Light blue shades chosen so black text stays readable.
        TextColor buttonBlue = new TextColor.RGB(144, 202, 249);
        TextColor buttonFocusBlue = new TextColor.RGB(100, 181, 246);
        TextColor selectedBlue = new TextColor.RGB(187, 222, 251);

        SimpleTheme theme = SimpleTheme.makeTheme(
                true,          // active/focused styles are bold
                black,         // base foreground
                white,         // base background
                black,         // editable foreground
                white,         // editable background
                black,         // selected foreground
                selectedBlue,  // selected background
                white);        // GUI background

        // Buttons are blue in all normal states. A focused button is a deeper
        // blue, and the black label remains readable on both shades.
        theme.addOverride(Button.class, black, buttonBlue)
                .setPreLight(black, buttonBlue)
                .setSelected(black, buttonBlue)
                .setInsensitive(black, buttonBlue)
                .setActive(black, buttonFocusBlue);

        // A selected table row is blue. The row gets a stronger blue when the
        // table has keyboard focus.
        theme.addOverride(Table.class, black, white)
                .setSelected(black, selectedBlue)
                .setActive(black, buttonFocusBlue);

        // Input fields stay white even when focused. The blinking text caret
        // and bold text show which box is active.
        theme.addOverride(TextBox.class, black, white)
                .setActive(black, white, SGR.BOLD)
                .setSelected(black, white, SGR.BOLD);

        theme.addOverride(Label.class, black, white);
        theme.addOverride(AbstractBorder.class, black, white);
        return theme;
    }

    /**
     * Builds the three input rows. Each label has a fixed width so the text
     * boxes line up neatly.
     */
    private Panel createEntryPanel() {
        Panel form = new Panel(new LinearLayout(Direction.VERTICAL));
        form.addComponent(createInputRow("Course Code (e.g. COS 201):", codeBox));
        form.addComponent(createInputRow("Course Title:", titleBox));
        form.addComponent(createInputRow("Credit Units:", unitsBox));
        return form;
    }

    private static Panel createInputRow(String labelText, TextBox textBox) {
        Panel row = new Panel(new LinearLayout(Direction.HORIZONTAL).setSpacing(1));
        Label label = new Label(labelText);
        label.setLabelWidth(30);
        row.addComponent(label);
        row.addComponent(textBox);
        return row;
    }

    /**
     * Creates the seven required actions as buttons. They are split over two
     * rows so the labels stay readable at the chosen window size.
     */
    private Panel createActionButtons() {
        Panel firstRow = new Panel(new LinearLayout(Direction.HORIZONTAL).setSpacing(1));
        firstRow.addComponent(new Button("Add Course", this::handleAddCourse));
        firstRow.addComponent(new Button("View All Courses", this::handleViewAllCourses));
        firstRow.addComponent(new Button("Search Course by Code", this::handleSearchCourse));
        firstRow.addComponent(new Button("Compute Total Units", this::handleComputeTotalUnits));

        Panel secondRow = new Panel(new LinearLayout(Direction.HORIZONTAL).setSpacing(1));
        secondRow.addComponent(new Button("Save to File", this::handleSaveToFile));
        secondRow.addComponent(new Button("Load from File", this::handleLoadFromFile));
        secondRow.addComponent(new Button("Exit Program", this::requestExit));

        Panel buttons = new Panel(new LinearLayout(Direction.VERTICAL).setSpacing(1));
        buttons.addComponent(firstRow);
        buttons.addComponent(secondRow);
        return buttons;
    }

    private static Border titledBorder(String title, Component content) {
        Border border = Borders.singleLine(title);
        border.setComponent(content);
        return border;
    }

    /**
     * Action 1: validate and add a course, then refresh the table and status.
     * Errors are reported inline and never close the application.
     */
    private void handleAddCourse() {
        try {
            int units = Course.parseUnits(unitsBox.getText());
            Course added = courseManager.addCourse(
                    codeBox.getText(),
                    titleBox.getText(),
                    units);

            unsavedChanges = true;
            clearEntryFields();
            refreshView();
            setStatus("Added " + added.getCode() + " - " + added.getTitle()
                    + " (" + added.getUnits() + (added.getUnits() == 1 ? " unit)." : " units)."));
            codeBox.takeFocus();
        } catch (IllegalArgumentException e) {
            setStatus("Cannot add course: " + e.getMessage());
        }
    }

    /**
     * Action 2: refresh and report the current course list.
     */
    private void handleViewAllCourses() {
        refreshView();
        int count = courseManager.getCourseCount();
        if (count == 0) {
            setStatus("No courses recorded yet. Add a course or load from a file.");
        } else {
            setStatus("Showing " + count + (count == 1 ? " course." : " courses."));
        }
    }

    /**
     * Action 3: ask for a code and use the recursive search in CourseManager.
     */
    private void handleSearchCourse() {
        String input = TextInputDialog.showDialog(
                gui,
                "Search Course by Code",
                "Enter a course code (for example, COS 201):",
                codeBox.getText());

        if (input == null) {
            setStatus("Search cancelled.");
            return;
        }

        try {
            Course found = courseManager.searchCourse(input);
            if (found == null) {
                String normalized = Course.normalizeCode(input);
                setStatus("No course found with code " + normalized + ".");
            } else {
                setStatus("Found " + found.getCode() + " - " + found.getTitle()
                        + " (" + found.getUnits()
                        + (found.getUnits() == 1 ? " unit)." : " units)."));
                highlightCourse(found);
            }
        } catch (IllegalArgumentException e) {
            setStatus("Cannot search: " + e.getMessage());
        }
    }

    /**
     * Action 4: calculate the total with a loop in CourseManager.
     */
    private void handleComputeTotalUnits() {
        refreshView();
        int count = courseManager.getCourseCount();
        int total = courseManager.computeTotalUnits();

        if (count == 0) {
            setStatus("Total credit units: 0 (no courses recorded).");
        } else {
            setStatus("Total credit units: " + total + " across " + count
                    + (count == 1 ? " course." : " courses."));
        }
    }

    /**
     * Action 5: save a complete snapshot. The success message is only shown
     * after the file handler finishes writing and replacing the file.
     */
    private void handleSaveToFile() {
        try {
            fileHandler.saveCourses(courseManager.getAllCourses());
            unsavedChanges = false;
            refreshView();
            int count = courseManager.getCourseCount();
            setStatus("Saved " + count + (count == 1 ? " course" : " courses")
                    + " to " + fileHandler.getDisplayPath() + ".");
        } catch (IOException e) {
            refreshView();
            setStatus("Could not save to " + fileHandler.getDisplayPath() + ": " + e.getMessage());
        }
    }

    /**
     * Action 6: load a file. Unsaved work is protected by a confirmation, and
     * the current list is only replaced after the whole file loads correctly.
     */
    private void handleLoadFromFile() {
        if (unsavedChanges) {
            MessageDialogButton answer = MessageDialog.showMessageDialog(
                    gui,
                    "Unsaved Changes",
                    "Loading will replace the current course list. Continue?",
                    MessageDialogButton.Yes,
                    MessageDialogButton.No);

            if (answer != MessageDialogButton.Yes) {
                setStatus("Load cancelled. Your current courses are unchanged.");
                return;
            }
        }

        try {
            List<Course> loadedCourses = fileHandler.loadCourses();
            courseManager.replaceAllCourses(loadedCourses);
            unsavedChanges = false;
            refreshView();
            int count = courseManager.getCourseCount();
            setStatus("Loaded " + count + (count == 1 ? " course" : " courses")
                    + " from " + fileHandler.getDisplayPath() + ".");
        } catch (IOException e) {
            // The manager was not touched, so existing courses remain intact.
            refreshView();
            setStatus("Could not load from " + fileHandler.getDisplayPath() + ": " + e.getMessage());
        }
    }

    /**
     * Action 7: exit safely. If there are unsaved changes the user can save,
     * discard, or cancel and keep working.
     *
     * <p>This method is also called by the Swing window close button in Main.</p>
     */
    public void requestExit() {
        if (unsavedChanges) {
            MessageDialogButton answer = MessageDialog.showMessageDialog(
                    gui,
                    "Unsaved Changes",
                    "You have unsaved changes. Save before exiting?",
                    MessageDialogButton.Yes,
                    MessageDialogButton.No,
                    MessageDialogButton.Cancel);

            if (answer == null || answer == MessageDialogButton.Cancel) {
                setStatus("Exit cancelled.");
                return;
            }

            if (answer == MessageDialogButton.Yes) {
                try {
                    fileHandler.saveCourses(courseManager.getAllCourses());
                    unsavedChanges = false;
                } catch (IOException e) {
                    setStatus("Could not save before exiting: " + e.getMessage());
                    return;
                }
            }
        }

        if (window != null) {
            window.close();
        }
    }

    /**
     * Rebuilds the table and status summary from the manager's read-only list.
     */
    private void refreshView() {
        List<Course> courses = courseManager.getAllCourses();
        TableModel<String> model = courseTable.getTableModel();
        model.clear();

        for (Course course : courses) {
            model.addRow(course.getCode(), course.getTitle(), Integer.toString(course.getUnits()));
        }

        if (courses.isEmpty()) {
            tableHintLabel.setText("No courses recorded yet. Use Add Course or Load from File.");
        } else {
            tableHintLabel.setText("Use Up/Down to select a course in the table.");
            int selectedRow = courseTable.getSelectedRow();
            if (selectedRow < 0 || selectedRow >= courses.size()) {
                courseTable.setSelectedRow(0);
            }
        }

        summaryLabel.setText("Courses: " + courses.size()
                + "    Total units: " + courseManager.computeTotalUnits()
                + "    Unsaved changes: " + (unsavedChanges ? "Yes" : "No"));
    }

    /**
     * Moves the table selection to the course found by the search.
     */
    private void highlightCourse(Course course) {
        List<Course> courses = courseManager.getAllCourses();
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).getCode().equals(course.getCode())) {
                courseTable.setSelectedRow(i);
                courseTable.takeFocus();
                return;
            }
        }
    }

    private void clearEntryFields() {
        codeBox.setText("");
        titleBox.setText("");
        unitsBox.setText("");
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
