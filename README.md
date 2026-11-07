# Student Course Management (COS 201 Mini-System)

A small Java desktop application for managing a student's course list. The user
interface is a text-based Lanterna interface displayed inside a normal Swing
window, so it can be launched directly from IntelliJ IDEA.

The project keeps the design intentionally small: one model class, one service
class, one file handler, one UI class, and one entry point. It does not use a
database, a web server, Spring, authentication, or any other unrelated feature.

## Features

The application provides all seven required actions:

1. Add Course
2. View All Courses
3. Search Course by Code
4. Compute Total Units
5. Save to File
6. Load from File
7. Exit Program

Additional behaviour:

- Course codes are normalized, so `cos201`, `COS 201`, and ` cos  201 ` all
  become `COS 201`.
- Duplicate course codes are rejected.
- Blank titles, invalid codes, zero/negative units, nonnumeric units, and
  integer overflow are rejected with clear status messages.
- Search is implemented recursively.
- Total units are computed with a normal loop.
- Saving writes a complete snapshot, not an append, so repeated saves do not
  create duplicate records.
- Loading validates the entire file before replacing the current list. A bad
  line reports its line number and leaves the existing list unchanged.
- Unsaved changes are tracked, and load/exit actions ask before discarding or
  replacing data.

## Requirements

- JDK 27. The Maven configuration uses `<maven.compiler.release>27</maven.compiler.release>`.
- A desktop environment. The application uses Lanterna's Swing terminal.
- No global Maven installation is required because the Maven Wrapper is included.
- The dependency `com.googlecode.lanterna:lanterna:3.1.5` is downloaded from
  Maven Central on the first build.

## Quick Start (PowerShell)

From the project root:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd exec:java
```

The application opens in a Lanterna/Swing window. Saved records are written to
`data\courses.txt` relative to the working directory. The file is not loaded
automatically at startup; use **Load from File** after starting the program.

If you use the classic Command Prompt instead of PowerShell, run:

```bat
mvnw.cmd clean package
mvnw.cmd exec:java
```

## Run in IntelliJ IDEA

1. Open IntelliJ IDEA and choose **File > Open**.
2. Select the project folder `Student Course Management (Mini-System)` itself,
   not just the `pom.xml` file, and click **OK**.
3. If IntelliJ asks whether to trust the project, choose **Trust Project**.
4. Wait for indexing and Maven import to finish. If it does not start
   automatically, open the **Maven** tool window and click
   **Reload All Maven Projects**.
5. Set the project JDK:
   - Open **File > Project Structure > Project**.
   - Set **SDK** to JDK 27.
   - Set **Language level** to **27**.
6. Create a run configuration:
   - Open **Run > Edit Configurations**.
   - Click **+** and choose **Application**.
   - Name: `Student Course Management`
   - Main class: `com.simon.coursemanagement.Main`
   - Module: `student-course-management`
   - JRE: JDK 27
   - Working directory: the project root
     (`C:\Users\OYALE\Desktop\Project\Student Course Management (Mini-System)`)
7. Click **Apply**, then **OK**.
8. Run the configuration with **Run > Run 'Student Course Management'**.

The working directory must be the project root if you want the default
`data\courses.txt` path to resolve there. IntelliJ normally uses the project root
for a newly created Application configuration.

If IntelliJ reports that `Main` cannot be found, run the wrapper build once and
reload the Maven project:

```powershell
.\mvnw.cmd clean package
```

## Using the Application

- Use **Tab** and **Shift+Tab** to move between input fields and buttons.
- Press **Enter** or **Space** to activate a focused button.
- Use **Up/Down** to select a row in the course table.
- The status area at the bottom reports success and error messages.
- The summary line shows the number of courses, total units, and whether there
  are unsaved changes.
- **Save to File** writes the current list to `data\courses.txt`.
- **Load from File** replaces the current list only after the whole file has
  been validated.
- **Exit Program** and the window close button both check for unsaved changes.

### Sample data

A ready-made sample file is included at `data\sample-courses.txt`. To try it,
copy it over the live file:

```powershell
Copy-Item -LiteralPath "data\sample-courses.txt" -Destination "data\courses.txt" -Force
```

Then start the application and choose **Load from File**.

Do not store your only copy of the data in `data\sample-courses.txt`; that file
is intended to stay as a reusable sample. The live file `data\courses.txt` is
ignored by Git.

### Saved file format

The file is UTF-8 text with one record per line and exactly three tab-separated
fields:

```text
CODE<TAB>TITLE<TAB>UNITS
```

For example (the separators shown as `\t` here are real tab characters in the
file):

```text
COS 201\tIntroduction to Programming\t3
MTH 201\tMathematics for Computing\t4
```

Tabs and line breaks are rejected in course codes and titles so a saved record
can never be split into the wrong fields.

## Verification

The project includes a plain-Java verification program with no test-only
dependencies. It uses temporary files, so it does not touch `data\courses.txt`.

Run the checks from PowerShell:

```powershell
.\mvnw.cmd -q clean test-compile
$lanterna = Join-Path $env:USERPROFILE ".m2\repository\com\googlecode\lanterna\lanterna\3.1.5\lanterna-3.1.5.jar"
java -cp "target\test-classes;target\classes;$lanterna" com.simon.coursemanagement.VerificationChecks
```

Expected output:

```text
ALL 56 CHECKS PASSED
```

The checks cover:

- Adding and listing courses
- Code normalization and case/space-insensitive search
- Duplicate prevention
- Blank, malformed, tab/line-break, zero, negative, nonnumeric, and overflow
  input rejection
- Recursive search on first, middle, and last records, plus the empty-list case
- Total-unit calculation
- UTF-8 save/load round trip
- Missing-file error reporting
- Malformed and duplicate file rows with line numbers
- Failed loads leaving existing courses unchanged
- Snapshot saving without appending
- Read-only exposure of the internal list
- Cleanup of temporary save files

Build and package verification:

```powershell
.\mvnw.cmd clean package
```

A successful run ends with `BUILD SUCCESS`.

The automated checks cover business logic and file handling. They do not replace
a manual visual check of the Lanterna window; launch the application and inspect
the layout, focus states, and button behaviour before submitting screenshots.

## Screenshots for Submission

A screenshot of the real running application is included at
`docs\screenshots\01-main-window.png`. It shows the initial empty state of the
main window. For a more useful submission screenshot, add a few courses or load
the sample file, then capture the window again.

![Main window](docs/screenshots/01-main-window.png)

1. Start the application and add a few courses, or load
   `data\sample-courses.txt`.
2. Press **Win+Shift+S** on Windows.
3. Select the application window and save the image under
   `docs\screenshots\`.
4. Suggested captures:
   - `01-main-window.png` - the complete main window (included in the project)
   - `02-main-window-with-courses.png` - the table populated with records
   - `03-search-result.png` - a successful course search
   - `04-total-units.png` - the total-units status message
   - `05-save-success.png` - the save confirmation
   - `06-load-error.png` - a malformed-file or missing-file error
5. Use screenshots of the real running application only. Do not fabricate
   results.

## Project Layout

```text
.
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- README.md
|-- .gitignore
|-- .mvn/
|   `-- wrapper/
|       `-- maven-wrapper.properties
|-- data/
|   `-- sample-courses.txt
|-- docs/
|   |-- COS 201 Lab Assessment.pdf
|   |-- Student_Course_Management_Project_Plan.pdf
|   |-- report-draft.md
|   `-- screenshots/
|       `-- 01-main-window.png
|-- src/
|   |-- main/java/com/simon/coursemanagement/
|   |   |-- Main.java
|   |   |-- model/Course.java
|   |   |-- service/CourseManager.java
|   |   |-- storage/CourseFileHandler.java
|   |   `-- ui/ConsoleMenu.java
|   `-- test/java/com/simon/coursemanagement/
|       `-- VerificationChecks.java
`-- target/                 (generated by Maven, not committed)
```

## Design Assumptions

- The course code format is intentionally project-specific: exactly three
  letters followed by exactly three digits. This is not claimed to be a
  university-wide rule.
- Course codes are stored with a single space, for example `COS 201`.
- Records are stored in a plain UTF-8 text file, not a database.
- The application is single-user and does not attempt concurrent file access.
- There is no edit or delete action because those features were not part of the
  required seven actions.
- The application does not load data automatically at startup; this keeps
  startup predictable and lets the user choose when to replace the current list.

## Troubleshooting

**The window does not open**

- Make sure you are running on a desktop session, not a headless CI session.
- Check that JDK 27 is selected in IntelliJ and that the Maven import completed.

**`No saved file was found at data\courses.txt`**

- Add at least one course and use **Save to File**, or copy
  `data\sample-courses.txt` to `data\courses.txt`.

**`Could not load ... Line N`**

- Open the saved file and fix the reported line. It must contain exactly three
  tab-separated fields: code, title, and units.
- The current in-memory list is not changed by a failed load.

**The build uses the wrong Java version**

- Confirm the project SDK is JDK 27.
- The project intentionally targets Java 27 through `maven.compiler.release`.
  To use an older JDK, lower that value in `pom.xml` and use a matching JDK.

**The layout looks cramped**

- The initial Swing terminal is 110 columns by 34 rows. The window and font size
  are configured in `Main.java`.
