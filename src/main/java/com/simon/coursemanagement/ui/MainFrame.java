package com.simon.coursemanagement.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.simon.coursemanagement.model.Course;
import com.simon.coursemanagement.service.CourseManager;
import com.simon.coursemanagement.storage.CourseFileHandler;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Main window for the course manager. */
public final class MainFrame extends JFrame {

    private static final int FIELD_HEIGHT = 40;
    private static final int CONTENT_GAP = 12;
    private static final int BUTTON_HEIGHT = 40;
    private static final String OUTLINE_PROPERTY = "JComponent.outline";
    private static final String OUTLINE_ERROR = "error";

    private final CourseManager courseManager;
    private final CourseFileHandler fileHandler;

    private final CourseTableModel tableModel = new CourseTableModel();
    private final JTable courseTable = new JTable(tableModel);

    private final JTextField codeField = new JTextField();
    private final JTextField titleField = new JTextField();
    private final JTextField unitsField = new JTextField();
    private final JTextField searchField = new JTextField();

    private final JLabel courseCountValue = new JLabel("0");
    private final JLabel totalUnitsValue = new JLabel("0");
    private final JLabel saveStatusValue = new JLabel("All changes saved");
    private final JLabel formMessageLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JLabel entryTitleLabel = new JLabel("Course Entry");
    private final JLabel entrySubtitleLabel = new JLabel("Add a validated course record.");
    private final JLabel filePathLabel;

    private final AppButton addButton = sized(AppButton.primary("Add Course"), 135);
    private final AppButton clearButton = sized(AppButton.secondary("Clear Form"), 115);
    private final AppButton searchButton = sized(AppButton.primary("Search"), 100);
    private final AppButton showAllButton = sized(AppButton.secondary("Show All"), 105);
    private final AppButton editSelectedButton = sized(AppButton.secondary("Edit Selected"), 125);
    private final AppButton deleteSelectedButton = sized(AppButton.secondary("Delete Selected"), 140);
    private final AppButton totalButton = sized(AppButton.secondary("Compute Total Units"), 175);
    private final AppButton saveButton = sized(AppButton.primary("Save to File"), 135);
    private final AppButton loadButton = sized(AppButton.secondary("Load from File"), 140);

    private final AppButton dashboardButton = sidebarButton("Dashboard");
    private final AppButton coursesButton = sidebarButton("Courses");
    private final AppButton exitButton = sidebarButton("Exit");

    private final CardLayout tableCards = new CardLayout();
    private final JPanel tableCardPanel = new JPanel(tableCards);
    private final JLabel emptyTitleLabel = new JLabel();
    private final JLabel emptyHintLabel = new JLabel();

    private final List<JButton> actionButtons = new ArrayList<>();
    private final List<Course> displayedCourses = new ArrayList<>();

    private AppButton activeNavigationButton;
    private Course editingCourse;
    private boolean searchActive;
    private boolean unsavedChanges;
    private boolean busy;
    private boolean exiting;

    /** Builds the window. Must run on the Swing event thread. */
    public MainFrame(CourseManager courseManager, CourseFileHandler fileHandler) {
        super("Course Manager - Student Course Management");
        if (courseManager == null || fileHandler == null) {
            throw new IllegalArgumentException("MainFrame dependencies cannot be null.");
        }
        this.courseManager = courseManager;
        this.fileHandler = fileHandler;
        this.filePathLabel = new JLabel("File: " + fileHandler.getDisplayPath());

        actionButtons.addAll(List.of(
                addButton, clearButton, searchButton, showAllButton,
                editSelectedButton, deleteSelectedButton,
                totalButton, saveButton, loadButton, exitButton));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setSize(1240, 700);
        setLocationRelativeTo(null);
        setContentPane(buildRootPanel());
        installFocusOrder();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestExit();
            }
        });

        refreshSummary();
        refreshTable();
        updateActionAvailability();
        setStatus("Ready. Add a course or load a saved file.", StatusKind.INFO);

        // Focus the code field after the window is visible.
        SwingUtilities.invokeLater(codeField::requestFocusInWindow);
    }

    /** Builds the sidebar and main content area. */
    private JPanel buildRootPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.WHITE);
        root.add(createSidebar(), BorderLayout.WEST);
        root.add(createMainArea(), BorderLayout.CENTER);
        return root;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UiTheme.NAVY);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(new EmptyBorder(26, 20, 22, 20));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("Course Manager");
        appName.setFont(UiTheme.font(22, Font.BOLD));
        appName.setForeground(UiTheme.WHITE);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appSubtitle = new JLabel("COS 201 - Semester Records");
        appSubtitle.setFont(UiTheme.font(12, Font.PLAIN));
        appSubtitle.setForeground(UiTheme.NAVY_MUTED);
        appSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel accent = new JPanel();
        accent.setBackground(UiTheme.BLUE);
        accent.setPreferredSize(new Dimension(34, 3));
        accent.setMaximumSize(new Dimension(34, 3));
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(appName);
        brand.add(Box.createVerticalStrut(5));
        brand.add(appSubtitle);
        brand.add(Box.createVerticalStrut(16));
        brand.add(accent);

        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));

        dashboardButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        coursesButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        dashboardButton.addActionListener(event -> navigateToDashboard());
        coursesButton.addActionListener(event -> navigateToCourses());

        navigation.add(dashboardButton);
        navigation.add(Box.createVerticalStrut(6));
        navigation.add(coursesButton);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(40, 0, 0, 0));
        center.add(navigation, BorderLayout.NORTH);
        center.add(createSidebarFooter(), BorderLayout.SOUTH);

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(center, BorderLayout.CENTER);

        setActiveNavigation(dashboardButton);
        return sidebar;
    }

    private JPanel createSidebarFooter() {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JPanel separator = new JPanel();
        separator.setBackground(UiTheme.NAVY_HOVER);
        separator.setPreferredSize(new Dimension(200, 1));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JLabel versionLabel = new JLabel("COS 201 Lab Assessment");
        versionLabel.setFont(UiTheme.font(11, Font.PLAIN));
        versionLabel.setForeground(UiTheme.NAVY_MUTED);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exitButton.addActionListener(event -> requestExit());

        footer.add(separator);
        footer.add(Box.createVerticalStrut(14));
        footer.add(exitButton);
        footer.add(Box.createVerticalStrut(18));
        footer.add(versionLabel);
        return footer;
    }

    private JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UiTheme.WHITE);

        JPanel body = new JPanel(new BorderLayout(0, CONTENT_GAP));
        body.setOpaque(false);
        body.add(createSummaryPanel(), BorderLayout.NORTH);
        body.add(createWorkspacePanel(), BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, CONTENT_GAP));
        content.setBackground(UiTheme.WHITE);
        content.setBorder(new EmptyBorder(16, 30, 16, 30));
        content.add(createHeader(), BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);

        JPanel fileActionsWrapper = new JPanel(new BorderLayout());
        fileActionsWrapper.setBackground(UiTheme.WHITE);
        fileActionsWrapper.setBorder(new EmptyBorder(0, 30, 12, 30));
        fileActionsWrapper.add(createFileActionsPanel(), BorderLayout.CENTER);

        JPanel bottomArea = new JPanel(new BorderLayout());
        bottomArea.setBackground(UiTheme.WHITE);
        bottomArea.add(fileActionsWrapper, BorderLayout.CENTER);
        bottomArea.add(createStatusBar(), BorderLayout.SOUTH);

        main.add(content, BorderLayout.CENTER);
        main.add(bottomArea, BorderLayout.SOUTH);
        return main;
    }

    /** Places the entry form beside the course table. */
    private JPanel createWorkspacePanel() {
        JPanel workspace = new JPanel(new GridBagLayout());
        workspace.setOpaque(false);

        GridBagConstraints entryConstraints = new GridBagConstraints();
        entryConstraints.gridx = 0;
        entryConstraints.gridy = 0;
        entryConstraints.weightx = 0.38;
        entryConstraints.weighty = 1;
        entryConstraints.fill = GridBagConstraints.BOTH;
        entryConstraints.anchor = GridBagConstraints.NORTH;
        entryConstraints.insets = new Insets(0, 0, 0, 14);

        GridBagConstraints coursesConstraints = new GridBagConstraints();
        coursesConstraints.gridx = 1;
        coursesConstraints.gridy = 0;
        coursesConstraints.weightx = 0.62;
        coursesConstraints.weighty = 1;
        coursesConstraints.fill = GridBagConstraints.BOTH;
        coursesConstraints.anchor = GridBagConstraints.NORTH;

        workspace.add(createEntryPanel(), entryConstraints);
        workspace.add(createCoursesPanel(), coursesConstraints);
        return workspace;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                new EmptyBorder(0, 0, 14, 0)));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Student Course Management");
        title.setFont(UiTheme.font(28, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("COS 201 - Semester Course Records");
        subtitle.setFont(UiTheme.font(14, Font.PLAIN));
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(title);
        text.add(Box.createVerticalStrut(5));
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JPanel createSummaryPanel() {
        JPanel summary = new JPanel(new GridLayout(1, 3, 16, 0));
        summary.setOpaque(false);

        courseCountValue.setFont(UiTheme.font(24, Font.BOLD));
        courseCountValue.setForeground(UiTheme.TEXT);
        totalUnitsValue.setFont(UiTheme.font(24, Font.BOLD));
        totalUnitsValue.setForeground(UiTheme.TEXT);
        saveStatusValue.setFont(UiTheme.font(18, Font.BOLD));
        saveStatusValue.setForeground(UiTheme.TEXT);

        summary.add(createStatCard("COURSES", courseCountValue));
        summary.add(createStatCard("TOTAL CREDIT UNITS", totalUnitsValue));
        summary.add(createStatCard("SAVE STATUS", saveStatusValue));
        return summary;
    }

    private JPanel createStatCard(String labelText, JLabel valueLabel) {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(labelText);
        label.setFont(UiTheme.font(11, Font.BOLD));
        label.setForeground(UiTheme.MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(label);
        text.add(Box.createVerticalStrut(5));
        text.add(valueLabel);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel createEntryPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        entryTitleLabel.setFont(UiTheme.font(18, Font.BOLD));
        entryTitleLabel.setForeground(UiTheme.TEXT);
        entryTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        entrySubtitleLabel.setFont(UiTheme.font(13, Font.PLAIN));
        entrySubtitleLabel.setForeground(UiTheme.MUTED);
        entrySubtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(entryTitleLabel);
        heading.add(Box.createVerticalStrut(2));
        heading.add(entrySubtitleLabel);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addFormField(form, codeField, "Course Code", "e.g. COS 201", 0);
        addFormField(form, titleField, "Course Title", "e.g. Introduction to Programming", 1);
        addFormField(form, unitsField, "Credit Units", "e.g. 3", 2);

        JPanel footer = new JPanel(new BorderLayout(0, 4));
        footer.setOpaque(false);
        formMessageLabel.setFont(UiTheme.font(13, Font.BOLD));
        formMessageLabel.setForeground(UiTheme.ERROR);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(Box.createHorizontalGlue());
        buttons.add(clearButton);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(addButton);

        footer.add(formMessageLabel, BorderLayout.NORTH);
        footer.add(buttons, BorderLayout.SOUTH);

        card.add(heading, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        clearButton.addActionListener(event -> handleClearOrCancelForm());
        addButton.addActionListener(event -> handlePrimaryFormAction());
        return card;
    }

    private void addFormField(JPanel form,
                              JTextField field,
                              String labelText,
                              String placeholder,
                              int row) {
        configureTextField(field, placeholder);
        installFormFieldBehaviour(field);

        JLabel label = new JLabel(labelText);
        label.setFont(UiTheme.font(12, Font.BOLD));
        label.setForeground(UiTheme.TEXT);
        label.setLabelFor(field);

        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(0, 0, row < 2 ? 4 : 0, 0);
        form.add(group, constraints);
    }

    private void configureTextField(JTextField field, String placeholder) {
        field.setFont(UiTheme.font(14, Font.PLAIN));
        field.setForeground(UiTheme.TEXT);
        field.setPreferredSize(new Dimension(160, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(90, FIELD_HEIGHT));
        field.setMargin(new Insets(0, 12, 0, 12));
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        field.setToolTipText(placeholder);
    }

    /** Adds Enter-to-submit and clears old validation messages. */
    private void installFormFieldBehaviour(JTextField field) {
        field.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit-course");
        field.getActionMap().put("submit-course", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleAddCourse();
            }
        });

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                clearFormError();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                clearFormError();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                clearFormError();
            }
        });
    }

    private JPanel createCoursesPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Recorded Courses");
        title.setFont(UiTheme.font(18, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Search, edit, or delete.");
        subtitle.setFont(UiTheme.font(13, Font.PLAIN));
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        configureTextField(searchField, "Search by course code");
        searchField.setPreferredSize(new Dimension(160, FIELD_HEIGHT));
        searchField.getAccessibleContext().setAccessibleName("Search course code");
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "search-course");
        searchField.getActionMap().put("search-course", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleSearch();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                clearOutline(searchField);
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                clearOutline(searchField);
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                clearOutline(searchField);
            }
        });

        searchButton.addActionListener(event -> handleSearch());
        showAllButton.addActionListener(event -> handleShowAll());
        editSelectedButton.addActionListener(event -> handleEditSelected());
        deleteSelectedButton.addActionListener(event -> handleDeleteSelected());

        JPanel headingRow = new JPanel(new BorderLayout(12, 0));
        headingRow.setOpaque(false);
        headingRow.add(heading, BorderLayout.WEST);
        headingRow.add(searchField, BorderLayout.EAST);

        JPanel actionRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 0));
        actionRow.setOpaque(false);
        actionRow.add(searchButton);
        actionRow.add(showAllButton);
        actionRow.add(editSelectedButton);
        actionRow.add(deleteSelectedButton);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(headingRow, BorderLayout.NORTH);
        top.add(actionRow, BorderLayout.SOUTH);

        configureCourseTable();
        tableCardPanel.setOpaque(false);
        tableCardPanel.setPreferredSize(new Dimension(0, 0));
        tableCardPanel.add(createTableScrollPane(), "table");
        tableCardPanel.add(createEmptyStatePanel(), "empty");

        card.add(top, BorderLayout.NORTH);
        card.add(tableCardPanel, BorderLayout.CENTER);
        card.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                card.revalidate();
            }
        });
        return card;
    }

    private void configureCourseTable() {
        courseTable.setFont(UiTheme.font(14, Font.PLAIN));
        courseTable.setForeground(UiTheme.TEXT);
        courseTable.setBackground(UiTheme.WHITE);
        courseTable.setRowHeight(36);
        courseTable.setShowVerticalLines(false);
        courseTable.setShowHorizontalLines(true);
        courseTable.setGridColor(UiTheme.BORDER);
        courseTable.setIntercellSpacing(new Dimension(0, 1));
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseTable.setSelectionBackground(UiTheme.BLUE);
        courseTable.setSelectionForeground(UiTheme.WHITE);
        courseTable.setFillsViewportHeight(true);
        courseTable.setRowSelectionAllowed(true);
        courseTable.setFocusable(true);
        courseTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        courseTable.setAutoCreateRowSorter(true);
        courseTable.setDefaultRenderer(Object.class, new CourseCellRenderer());
        courseTable.getAccessibleContext().setAccessibleName("Recorded courses table");
        courseTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActionAvailability();
            }
        });

        courseTable.getTableHeader().setReorderingAllowed(false);
        courseTable.getTableHeader().setFont(UiTheme.font(12, Font.BOLD));
        courseTable.getTableHeader().setBackground(UiTheme.SURFACE);
        courseTable.getTableHeader().setForeground(UiTheme.MUTED);
        courseTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        courseTable.getTableHeader().setDefaultRenderer(new CourseHeaderRenderer());

        courseTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        courseTable.getColumnModel().getColumn(0).setMinWidth(120);
        courseTable.getColumnModel().getColumn(0).setMaxWidth(190);
        courseTable.getColumnModel().getColumn(1).setPreferredWidth(520);
        courseTable.getColumnModel().getColumn(1).setMinWidth(220);
        courseTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        courseTable.getColumnModel().getColumn(2).setMinWidth(105);
        courseTable.getColumnModel().getColumn(2).setMaxWidth(160);
    }

    private JScrollPane createTableScrollPane() {
        JScrollPane scrollPane = new JScrollPane(courseTable);
        scrollPane.setBorder(new LineBorder(UiTheme.BORDER));
        scrollPane.getViewport().setBackground(UiTheme.WHITE);
        scrollPane.setPreferredSize(new Dimension(0, 0));
        return scrollPane;
    }

    private JPanel createEmptyStatePanel() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setBackground(UiTheme.WHITE);
        empty.setBorder(new LineBorder(UiTheme.BORDER));

        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));

        emptyTitleLabel.setFont(UiTheme.font(16, Font.BOLD));
        emptyTitleLabel.setForeground(UiTheme.TEXT);
        emptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyHintLabel.setFont(UiTheme.font(13, Font.PLAIN));
        emptyHintLabel.setForeground(UiTheme.MUTED);
        emptyHintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        message.add(emptyTitleLabel);
        message.add(Box.createVerticalStrut(6));
        message.add(emptyHintLabel);
        empty.add(message);
        return empty;
    }

    private JPanel createFileActionsPanel() {
        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("File Actions");
        title.setFont(UiTheme.font(18, Font.BOLD));
        title.setForeground(UiTheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(title);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(totalButton);
        buttons.add(loadButton);
        buttons.add(saveButton);

        totalButton.addActionListener(event -> handleComputeTotalUnits());
        saveButton.addActionListener(event -> handleSaveToFile());
        loadButton.addActionListener(event -> handleLoadFromFile());

        card.add(text, BorderLayout.WEST);
        card.add(buttons, BorderLayout.EAST);
        return card;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(16, 0));
        statusBar.setBackground(UiTheme.WHITE);
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                new EmptyBorder(8, 30, 8, 30)));

        statusLabel.setFont(UiTheme.font(13, Font.PLAIN));
        statusLabel.setForeground(UiTheme.MUTED);

        filePathLabel.setFont(UiTheme.font(12, Font.PLAIN));
        filePathLabel.setForeground(UiTheme.MUTED);

        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(filePathLabel, BorderLayout.EAST);
        return statusBar;
    }

    /** Runs Add or Save Changes depending on the current mode. */
    private void handlePrimaryFormAction() {
        if (editingCourse == null) {
            handleAddCourse();
        } else {
            handleUpdateCourse();
        }
    }

    /** Adds the course from the form. */
    private void handleAddCourse() {
        if (busy) {
            return;
        }

        Course validated = readValidatedFormCourse();
        if (validated == null) {
            return;
        }

        try {
            Course added = courseManager.addCourse(
                    validated.getCode(), validated.getTitle(), validated.getUnits());
            searchActive = false;
            displayedCourses.clear();
            searchField.setText("");
            setUnsavedChanges(true);
            clearEntryFields();
            refreshSummary();
            refreshTable();
            selectCourseByCode(added.getCode());
            setStatus("Added " + added.getCode() + " - " + added.getTitle()
                    + " (" + added.getUnits()
                    + (added.getUnits() == 1 ? " unit)." : " units)."), StatusKind.SUCCESS);
            codeField.requestFocusInWindow();
        } catch (IllegalArgumentException error) {
            showFormError(error.getMessage(), codeField);
        }
    }

    /** Saves changes to the course being edited. */
    private void handleUpdateCourse() {
        if (busy) {
            return;
        }

        Course original = editingCourse;
        if (original == null) {
            handleAddCourse();
            return;
        }

        Course validated = readValidatedFormCourse();
        if (validated == null) {
            return;
        }

        boolean changed = !hasSameValues(original, validated);
        try {
            Course updated = courseManager.updateCourse(
                    original, validated.getCode(), validated.getTitle(), validated.getUnits());

            stopEditing();
            if (searchActive) {
                refreshActiveSearch();
            } else {
                refreshTable();
            }
            refreshSummary();
            if (changed) {
                setUnsavedChanges(true);
            }
            selectCourseByCode(updated.getCode());

            if (changed) {
                setStatus("Updated " + updated.getCode() + " - " + updated.getTitle()
                        + " (" + updated.getUnits()
                        + (updated.getUnits() == 1 ? " unit)." : " units)."), StatusKind.SUCCESS);
            } else {
                setStatus("No changes were made to " + updated.getCode() + ".", StatusKind.INFO);
            }
        } catch (IllegalArgumentException error) {
            showFormError(error.getMessage(), codeField);
        }
    }

    /** Reads and validates the form, returning null if anything is invalid. */
    private Course readValidatedFormCourse() {
        clearFormError();
        clearInvalidOutlines();

        String codeText = codeField.getText();
        try {
            Course.normalizeCode(codeText);
        } catch (IllegalArgumentException error) {
            showFormError(error.getMessage(), codeField);
            return null;
        }

        String titleText = titleField.getText();
        try {
            Course.normalizeTitle(titleText);
        } catch (IllegalArgumentException error) {
            showFormError(error.getMessage(), titleField);
            return null;
        }

        int units;
        try {
            units = Course.parseUnits(unitsField.getText());
        } catch (IllegalArgumentException error) {
            showFormError(error.getMessage(), unitsField);
            return null;
        }

        try {
            return new Course(codeText, titleText, units);
        } catch (IllegalArgumentException error) {
            showFormError(error.getMessage(), codeField);
            return null;
        }
    }

    private static boolean hasSameValues(Course first, Course second) {
        return first.getCode().equals(second.getCode())
                && first.getTitle().equals(second.getTitle())
                && first.getUnits() == second.getUnits();
    }

    /** Loads the selected course into the form for editing. */
    private void handleEditSelected() {
        if (busy) {
            return;
        }

        Course selected = selectedCourse();
        if (selected == null) {
            setStatus("Select a course before choosing Edit Selected.", StatusKind.INFO);
            return;
        }

        editingCourse = selected;
        codeField.setText(selected.getCode());
        titleField.setText(selected.getTitle());
        unitsField.setText(Integer.toString(selected.getUnits()));
        clearFormError();
        clearInvalidOutlines();
        setEditingMode(true);
        setStatus("Editing " + selected.getCode() + " - " + selected.getTitle()
                + ". Change the fields and choose Save Changes.", StatusKind.INFO);
        codeField.requestFocusInWindow();
        codeField.selectAll();
    }

    /** Deletes the selected course after confirmation. */
    private void handleDeleteSelected() {
        if (busy) {
            return;
        }

        Course selected = selectedCourse();
        if (selected == null) {
            setStatus("Select a course before choosing Delete Selected.", StatusKind.INFO);
            return;
        }

        Object[] options = {"Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Delete " + selected.getCode() + " - " + selected.getTitle() + "?",
                "Delete Course",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
        if (choice != 0) {
            setStatus("Delete cancelled. No course was removed.", StatusKind.INFO);
            return;
        }

        try {
            Course deleted = courseManager.deleteCourse(selected);
            if (editingCourse == deleted) {
                stopEditing();
            }
            courseTable.clearSelection();
            setUnsavedChanges(true);
            if (searchActive) {
                refreshActiveSearch();
            } else {
                refreshTable();
            }
            refreshSummary();
            updateActionAvailability();
            setStatus("Deleted " + deleted.getCode() + " - " + deleted.getTitle()
                    + " (" + deleted.getUnits()
                    + (deleted.getUnits() == 1 ? " unit)." : " units)."), StatusKind.SUCCESS);
        } catch (IllegalArgumentException error) {
            setStatus(error.getMessage(), StatusKind.ERROR);
        }
    }

    /** Leaves edit mode and clears the form. */
    private void stopEditing() {
        editingCourse = null;
        setEditingMode(false);
        clearEntryFields();
        clearFormError();
        clearInvalidOutlines();
    }

    /** Updates the form text for add or edit mode. */
    private void setEditingMode(boolean editing) {
        entryTitleLabel.setText(editing ? "Edit Course" : "Course Entry");
        if (editing && editingCourse != null) {
            entrySubtitleLabel.setText("Editing " + editingCourse.getCode()
                    + ". Save changes or cancel.");
        } else {
            entrySubtitleLabel.setText("Add a validated course record.");
        }
        addButton.setText(editing ? "Save Changes" : "Add Course");
        clearButton.setText(editing ? "Cancel Edit" : "Clear Form");
    }

    /** Repeats the active search after an update or delete. */
    private void refreshActiveSearch() {
        displayedCourses.clear();
        if (searchActive) {
            try {
                String normalizedCode = Course.normalizeCode(searchField.getText());
                Course found = courseManager.searchCourse(normalizedCode);
                if (found != null) {
                    displayedCourses.add(found);
                }
            } catch (IllegalArgumentException ignored) {
                // Show an empty result instead of stale data.
            }
        }
        refreshTable();
        refreshSummary();
        updateActionAvailability();
    }

    /** Restores the full course list. */
    private void handleShowAll() {
        if (busy) {
            return;
        }
        searchActive = false;
        displayedCourses.clear();
        searchField.setText("");
        clearInvalidOutlines();
        refreshTable();
        refreshSummary();

        int count = courseManager.getCourseCount();
        if (count == 0) {
            setStatus("No courses recorded yet. Add a course or load from a file.", StatusKind.INFO);
        } else {
            setStatus("Showing all " + count + (count == 1 ? " course." : " courses."), StatusKind.INFO);
        }
    }

    /** Searches by code and filters the table to the match. */
    private void handleSearch() {
        if (busy) {
            return;
        }
        clearInvalidOutlines();

        String query = searchField.getText();
        String normalizedCode;
        try {
            normalizedCode = Course.normalizeCode(query);
        } catch (IllegalArgumentException error) {
            markInvalid(searchField);
            setStatus(error.getMessage(), StatusKind.ERROR);
            searchField.requestFocusInWindow();
            searchField.selectAll();
            return;
        }

        Course found = courseManager.searchCourse(normalizedCode);
        if (found == null) {
            searchActive = true;
            displayedCourses.clear();
            refreshTable();
            refreshSummary();
            setStatus("No course found with code " + normalizedCode + ".", StatusKind.INFO);
            return;
        }

        searchActive = true;
        displayedCourses.clear();
        displayedCourses.add(found);
        refreshTable();
        refreshSummary();
        selectCourseByCode(found.getCode());
        setStatus("Found " + found.getCode() + " - " + found.getTitle()
                + " (" + found.getUnits()
                + (found.getUnits() == 1 ? " unit)." : " units)."), StatusKind.SUCCESS);
    }

    /** Shows the total credit units. */
    private void handleComputeTotalUnits() {
        if (busy) {
            return;
        }
        refreshSummary();
        int count = courseManager.getCourseCount();
        int total = courseManager.computeTotalUnits();
        if (count == 0) {
            setStatus("Total credit units: 0 (no courses recorded).", StatusKind.INFO);
        } else {
            setStatus("Total credit units: " + total + " across " + count
                    + (count == 1 ? " course." : " courses."), StatusKind.SUCCESS);
        }
    }

    /** Saves the course list in the background. */
    private void handleSaveToFile() {
        if (busy) {
            return;
        }

        List<Course> snapshot = courseManager.getAllCourses();
        setBusy(true);
        setStatus("Saving " + snapshot.size()
                + (snapshot.size() == 1 ? " course" : " courses") + "...", StatusKind.BUSY);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                fileHandler.saveCourses(snapshot);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    setUnsavedChanges(false);
                    refreshSummary();
                    setStatus("Saved " + snapshot.size()
                            + (snapshot.size() == 1 ? " course" : " courses")
                            + " to " + fileHandler.getDisplayPath() + ".", StatusKind.SUCCESS);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    setStatus("Save interrupted. The current list is unchanged.", StatusKind.ERROR);
                } catch (ExecutionException error) {
                    String message = readableMessage(error.getCause());
                    setStatus("Could not save to " + fileHandler.getDisplayPath()
                            + ": " + message, StatusKind.ERROR);
                    showErrorDialog("Save failed", message);
                }
            }
        }.execute();
    }

    /** Loads the file in the background after confirming unsaved changes. */
    private void handleLoadFromFile() {
        if (busy) {
            return;
        }

        if (unsavedChanges) {
            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Loading will replace the current course list. Continue?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                setStatus("Load cancelled. Your current courses are unchanged.", StatusKind.INFO);
                return;
            }
        }

        setBusy(true);
        setStatus("Loading courses from " + fileHandler.getDisplayPath() + "...", StatusKind.BUSY);

        new SwingWorker<List<Course>, Void>() {
            @Override
            protected List<Course> doInBackground() throws IOException {
                return fileHandler.loadCourses();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    List<Course> loadedCourses = get();
                    courseManager.replaceAllCourses(loadedCourses);
                    stopEditing();
                    searchActive = false;
                    displayedCourses.clear();
                    searchField.setText("");
                    courseTable.clearSelection();
                    setUnsavedChanges(false);
                    refreshSummary();
                    refreshTable();
                    updateActionAvailability();
                    setStatus("Loaded " + loadedCourses.size()
                            + (loadedCourses.size() == 1 ? " course" : " courses")
                            + " from " + fileHandler.getDisplayPath() + ".", StatusKind.SUCCESS);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    setStatus("Load interrupted. Your current courses are unchanged.", StatusKind.ERROR);
                } catch (ExecutionException error) {
                    String message = readableMessage(error.getCause());
                    setStatus("Could not load from " + fileHandler.getDisplayPath()
                            + ": " + message, StatusKind.ERROR);
                    showErrorDialog("Load failed", message);
                }
            }
        }.execute();
    }

    /** Closes the window and protects unsaved changes. */
    public void requestExit() {
        if (exiting) {
            return;
        }
        if (busy) {
            setStatus("Please wait for the current file operation to finish.", StatusKind.INFO);
            return;
        }
        if (!unsavedChanges) {
            dispose();
            return;
        }

        Object[] options = {"Save and Exit", "Exit Without Saving", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "You have unsaved changes. Save before exiting?",
                "Unsaved Changes",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == JOptionPane.CLOSED_OPTION || choice == 2) {
            setStatus("Exit cancelled.", StatusKind.INFO);
            return;
        }
        if (choice == 1) {
            dispose();
            return;
        }
        saveBeforeExit();
    }

    private void saveBeforeExit() {
        exiting = true;
        setBusy(true);
        setStatus("Saving before exit...", StatusKind.BUSY);
        List<Course> snapshot = courseManager.getAllCourses();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                fileHandler.saveCourses(snapshot);
                return null;
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    setUnsavedChanges(false);
                    refreshSummary();
                    dispose();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    exiting = false;
                    setStatus("Exit interrupted. The window remains open.", StatusKind.ERROR);
                } catch (ExecutionException error) {
                    exiting = false;
                    String message = readableMessage(error.getCause());
                    setStatus("Could not save before exiting: " + message, StatusKind.ERROR);
                    showErrorDialog("Save failed", message);
                }
            }
        }.execute();
    }

    /** Clears the form or cancels edit mode. */
    private void handleClearOrCancelForm() {
        if (busy) {
            return;
        }
        if (editingCourse != null) {
            stopEditing();
            setStatus("Edit cancelled. The stored course was not changed.", StatusKind.INFO);
        } else {
            clearEntryFields();
            clearFormError();
            clearInvalidOutlines();
            setStatus("Entry fields cleared. Recorded courses were not changed.", StatusKind.INFO);
        }
        codeField.requestFocusInWindow();
    }

    private void navigateToDashboard() {
        setActiveNavigation(dashboardButton);
        codeField.requestFocusInWindow();
    }

    private void navigateToCourses() {
        setActiveNavigation(coursesButton);
        searchField.requestFocusInWindow();
    }

    private void setActiveNavigation(AppButton button) {
        if (activeNavigationButton != null) {
            activeNavigationButton.setActive(false);
        }
        activeNavigationButton = button;
        activeNavigationButton.setActive(true);
    }

    /** Rebuilds the table and shows the empty state when needed. */
    private void refreshTable() {
        List<Course> source = searchActive
                ? new ArrayList<>(displayedCourses)
                : courseManager.getAllCourses();
        tableModel.setCourses(source);

        if (tableModel.getRowCount() == 0) {
            if (searchActive) {
                emptyTitleLabel.setText("No matching courses.");
                emptyHintLabel.setText("Try another course code or choose Show All.");
            } else {
                emptyTitleLabel.setText("No courses recorded yet.");
                emptyHintLabel.setText("Use the form above to add your first course.");
            }
            tableCards.show(tableCardPanel, "empty");
        } else {
            tableCards.show(tableCardPanel, "table");
        }
        updateActionAvailability();
    }

    private void refreshSummary() {
        courseCountValue.setText(Integer.toString(courseManager.getCourseCount()));
        totalUnitsValue.setText(Integer.toString(courseManager.computeTotalUnits()));
        saveStatusValue.setText(unsavedChanges ? "Unsaved changes" : "All changes saved");
    }

    private void setUnsavedChanges(boolean unsavedChanges) {
        this.unsavedChanges = unsavedChanges;
        refreshSummary();
    }

    /** Returns the selected course, accounting for table sorting. */
    private Course selectedCourse() {
        int viewRow = courseTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = courseTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            return null;
        }
        return tableModel.getCourseAt(modelRow);
    }

    /** Enables or disables actions based on selection and busy state. */
    private void updateActionAvailability() {
        boolean enabled = !busy;
        for (JButton button : actionButtons) {
            button.setEnabled(enabled);
        }

        boolean hasSelection = selectedCourse() != null;
        editSelectedButton.setEnabled(enabled && hasSelection);
        deleteSelectedButton.setEnabled(enabled && hasSelection);
    }

    private void selectCourseByCode(String code) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (tableModel.getCourseAt(row).getCode().equals(code)) {
                int viewRow = courseTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    courseTable.setRowSelectionInterval(viewRow, viewRow);
                    Rectangle cell = courseTable.getCellRect(viewRow, 0, true);
                    courseTable.scrollRectToVisible(cell);
                    updateActionAvailability();
                }
                return;
            }
        }
        courseTable.clearSelection();
        updateActionAvailability();
    }

    private void clearEntryFields() {
        codeField.setText("");
        titleField.setText("");
        unitsField.setText("");
    }

    private void clearFormError() {
        formMessageLabel.setText(" ");
        formMessageLabel.setToolTipText(null);
        clearInvalidOutlines();
    }

    private void showFormError(String message, JTextField fieldToFocus) {
        formMessageLabel.setText("Error: Check the highlighted field.");
        formMessageLabel.setToolTipText(message);
        setStatus(message, StatusKind.ERROR);
        markInvalid(fieldToFocus);
        fieldToFocus.requestFocusInWindow();
        fieldToFocus.selectAll();
    }

    private void markInvalid(JTextField field) {
        field.putClientProperty(OUTLINE_PROPERTY, OUTLINE_ERROR);
        field.repaint();
    }

    private void clearInvalidOutlines() {
        clearOutline(codeField);
        clearOutline(titleField);
        clearOutline(unitsField);
        clearOutline(searchField);
    }

    private static void clearOutline(JTextField field) {
        field.putClientProperty(OUTLINE_PROPERTY, null);
        field.repaint();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        setCursor(Cursor.getPredefinedCursor(
                busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        updateActionAvailability();
    }

    private void setStatus(String message, StatusKind kind) {
        statusLabel.setText(kind.prefix + message);
        statusLabel.setForeground(kind.color);
        statusLabel.setToolTipText(message);
    }

    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static String readableMessage(Throwable error) {
        if (error == null) {
            return "Unknown error.";
        }
        String message = error.getMessage();
        return message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    private static AppButton sized(AppButton button, int width) {
        button.setPreferredSize(new Dimension(width, BUTTON_HEIGHT));
        return button;
    }

    private static AppButton sidebarButton(String text) {
        AppButton button = AppButton.sidebar(text);
        button.setPreferredSize(new Dimension(200, 42));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return button;
    }

    /** Sets a predictable Tab order. */
    private void installFocusOrder() {
        List<Component> order = List.of(
                dashboardButton, coursesButton,
                codeField, titleField, unitsField, addButton, clearButton,
                searchField, searchButton, showAllButton, courseTable,
                editSelectedButton, deleteSelectedButton,
                totalButton, saveButton, loadButton, exitButton);
        setFocusTraversalPolicy(new OrderedFocusTraversalPolicy(order));
    }

    private enum StatusKind {
        INFO("Info: ", UiTheme.MUTED),
        SUCCESS("Success: ", UiTheme.SUCCESS),
        ERROR("Error: ", UiTheme.ERROR),
        BUSY("Working: ", UiTheme.BLUE);

        private final String prefix;
        private final Color color;

        StatusKind(String prefix, Color color) {
            this.prefix = prefix;
            this.color = color;
        }
    }

    private static final class WrapLayout extends FlowLayout {

        private WrapLayout(int alignment, int horizontalGap, int verticalGap) {
            super(alignment, horizontalGap, verticalGap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= getHgap() + 1;
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth <= 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + getHgap() * 2;
                int maxWidth = targetWidth - horizontalInsetsAndGap;
                Dimension dimension = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                for (int index = 0; index < target.getComponentCount(); index++) {
                    Component component = target.getComponent(index);
                    if (!component.isVisible()) {
                        continue;
                    }

                    Dimension componentSize = preferred
                            ? component.getPreferredSize()
                            : component.getMinimumSize();
                    if (rowWidth + componentSize.width > maxWidth) {
                        addRow(dimension, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += componentSize.width;
                    rowHeight = Math.max(rowHeight, componentSize.height);
                }

                addRow(dimension, rowWidth, rowHeight);
                dimension.width += horizontalInsetsAndGap;
                dimension.height += insets.top + insets.bottom + getVgap() * 2;

                Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
                if (scrollPane != null && target.isValid()) {
                    dimension.width -= getHgap() + 1;
                }
                return dimension;
            }
        }

        private void addRow(Dimension dimension, int rowWidth, int rowHeight) {
            dimension.width = Math.max(dimension.width, rowWidth);
            if (dimension.height > 0) {
                dimension.height += getVgap();
            }
            dimension.height += rowHeight;
        }
    }

    /** Rounded white panel used for dashboard cards. */
    private static final class CardPanel extends JPanel {

        private CardPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UiTheme.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(UiTheme.BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Table cell renderer with padding and a bold course code. */
    private static final class CourseCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(0, 12, 0, 12));
            label.setFont(UiTheme.font(14, column == 0 ? Font.BOLD : Font.PLAIN));
            label.setHorizontalAlignment(column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
            if (isSelected) {
                label.setBackground(UiTheme.BLUE);
                label.setForeground(UiTheme.WHITE);
            } else {
                label.setBackground(UiTheme.WHITE);
                label.setForeground(UiTheme.TEXT);
            }
            return label;
        }
    }

    /** Table header renderer with sorting icons. */
    private static final class CourseHeaderRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setBorder(new EmptyBorder(0, 12, 0, 12));
            label.setFont(UiTheme.font(12, Font.BOLD));
            label.setBackground(UiTheme.SURFACE);
            label.setForeground(UiTheme.MUTED);
            label.setHorizontalAlignment(column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
            label.setIcon(sortIcon(table, column));
            return label;
        }

        private static Icon sortIcon(JTable table, int column) {
            RowSorter<?> sorter = table.getRowSorter();
            if (sorter == null) {
                return null;
            }
            List<? extends RowSorter.SortKey> keys = sorter.getSortKeys();
            if (keys.isEmpty() || keys.get(0).getColumn() != column) {
                return null;
            }
            if (keys.get(0).getSortOrder() == SortOrder.ASCENDING) {
                return UIManager.getIcon("Table.ascendingSortIcon");
            }
            if (keys.get(0).getSortOrder() == SortOrder.DESCENDING) {
                return UIManager.getIcon("Table.descendingSortIcon");
            }
            return null;
        }
    }

    /** Focus policy that skips disabled controls. */
    private static final class OrderedFocusTraversalPolicy extends FocusTraversalPolicy {

        private final List<Component> order;

        private OrderedFocusTraversalPolicy(List<Component> order) {
            this.order = order;
        }

        @Override
        public Component getComponentAfter(Container focusCycleRoot, Component component) {
            int index = order.indexOf(component);
            for (int offset = 1; offset <= order.size(); offset++) {
                Component candidate = order.get((index + offset) % order.size());
                if (isUsable(candidate)) {
                    return candidate;
                }
            }
            return defaultIfPossible();
        }

        @Override
        public Component getComponentBefore(Container focusCycleRoot, Component component) {
            int index = order.indexOf(component);
            if (index < 0) {
                index = 0;
            }
            for (int offset = 1; offset <= order.size(); offset++) {
                int candidateIndex = (index - offset + order.size()) % order.size();
                Component candidate = order.get(candidateIndex);
                if (isUsable(candidate)) {
                    return candidate;
                }
            }
            return defaultIfPossible();
        }

        @Override
        public Component getFirstComponent(Container focusCycleRoot) {
            for (Component component : order) {
                if (isUsable(component)) {
                    return component;
                }
            }
            return defaultIfPossible();
        }

        @Override
        public Component getLastComponent(Container focusCycleRoot) {
            for (int index = order.size() - 1; index >= 0; index--) {
                Component component = order.get(index);
                if (isUsable(component)) {
                    return component;
                }
            }
            return defaultIfPossible();
        }

        @Override
        public Component getDefaultComponent(Container focusCycleRoot) {
            return defaultIfPossible();
        }

        private Component defaultIfPossible() {
            for (Component component : order) {
                if (isUsable(component)) {
                    return component;
                }
            }
            return order.get(0);
        }

        private static boolean isUsable(Component component) {
            return component.isEnabled() && component.isVisible() && component.isShowing();
        }
    }
}
