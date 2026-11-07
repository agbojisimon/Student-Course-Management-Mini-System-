# COS 201 Programming I - Lab Assessment

## Student Course Management (Mini-System)

**Student:** [Your Name]  
**Index Number:** [Your Index Number]  
**Course:** COS 201 Programming I  
**Date:** [Date]

## 1. Introduction

This project implements a Student Course Management mini-system in Java. It
allows a student to add courses, view all records, search by course code,
compute total credit units, save the list to a file, load a saved list, and exit
safely. The interface is a Lanterna text-based UI shown in a Swing desktop
window. The program uses only the Java standard library and Lanterna `3.1.5`,
and it stores records in the UTF-8 text file `data/courses.txt`.

## 2. Program Structure

| Class | Responsibility |
| --- | --- |
| `Main` | Starts the Swing terminal, creates the application objects, applies the theme, and shuts down cleanly. |
| `Course` | Represents one course and validates/normalizes its code, title, and units. |
| `CourseManager` | Maintains the private `ArrayList<Course>`, prevents duplicates, searches recursively, and totals units. |
| `CourseFileHandler` | Saves and loads the tab-separated file with validation and safe replacement. |
| `ConsoleMenu` | Builds the UI and implements the seven required actions. |

A plain-Java `VerificationChecks` program in `src/test/java` tests the business
logic and file handling without adding test-only dependencies.

## 3. How the Main Features Work

### 3.1 Adding and viewing courses

The entry form collects a code, title, and units. The `Course` constructor
validates every value before an object can exist. Codes are trimmed, spaces are
removed, letters are uppercased with `Locale.ROOT`, and the result must match
three letters followed by three digits. For example, `cos201` becomes `COS 201`.
Titles must be nonblank, and units must be a positive whole number. The
`CourseManager` rejects duplicate codes. The table is rebuilt from the manager's
read-only snapshot whenever the list changes.

### 3.2 Searching and computing totals

`CourseManager.searchRecursive` searches one list position at a time:

- **Base case:** the index reaches the end, so return `null`.
- **Match case:** the current course code matches, so return that course.
- **Recursive step:** call the method with `index + 1`.

This is the required recursive feature and is also used for duplicate-code
detection. Total units are calculated with a normal `for` loop, as required.

### 3.3 Saving and loading

The file format is one record per line with three tab-separated fields:
`CODE<TAB>TITLE<TAB>UNITS`. Saving writes the whole list as a snapshot, not an
append. It writes to a temporary file and moves it over the real file, using an
atomic move when supported. Loading parses the entire file into a temporary
manager and reports the line number of any bad row. The current list is replaced
only after the whole file succeeds, so a failed load cannot destroy existing
data.

### 3.4 User interface and error handling

`ConsoleMenu` provides buttons, an entry form, a course table, a summary line,
and a status area. The custom theme uses a white background, black text, and
light blue buttons; focused and selected controls use a stronger blue. Tab and
Shift+Tab move between controls, and Up/Down select table rows. Invalid input is
shown in the status area rather than closing the program. Loading asks before
replacing unsaved work, and exit offers Save, Discard, or Cancel. The Swing
window close button uses the same exit routine.

## 4. Challenges Encountered

The main challenge was preventing invalid or damaging data. Validation is
centralized in `Course` and `Course.parseUnits`, and callers catch
`IllegalArgumentException` and display a readable message. Tabs and line breaks
are rejected in codes and titles so they cannot break the tab-separated format.

A second challenge was file safety. A partially written save could corrupt the
data file, so the handler writes to a temporary file and replaces the real file
only after writing succeeds. A malformed load could erase the live list, so the
loader validates everything first and replaces the live manager only afterward.
Missing files, malformed fields, invalid units, and duplicate codes are reported
with clear messages, including line numbers where relevant.

A third challenge was the user interface. The Lanterna Swing frame starts
hidden, so the program explicitly makes it visible after starting the screen.
Unsaved changes are tracked, and dialogs protect the user from losing work.

## 5. Testing and Verification

The project builds with the Maven Wrapper:

```text
.\mvnw.cmd clean package
.\mvnw.cmd -q clean test-compile
```

The verification program reported `ALL 56 CHECKS PASSED`. It covers adding and
listing courses, normalization, duplicate prevention, invalid and overflow
input, recursive search (first, middle, last, missing, and empty list), total
units, UTF-8 save/load round trip, missing files, malformed and duplicate file
rows, preservation of existing courses after a failed load, snapshot saving,
read-only list exposure, and temporary-file cleanup. The checks use temporary
directories and do not modify the live data file.

A screenshot of the running main window is included at
`docs/screenshots/01-main-window.png`. It shows the initial empty state. A
populated screenshot should be captured for submission if evidence of course
operations is required.

## 6. How to Run

From the project root:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd exec:java
```

In IntelliJ IDEA, open the project folder, select JDK 27, and create an
Application configuration with main class `com.simon.coursemanagement.Main` and
working directory set to the project root. Full instructions are in `README.md`.
To try the sample records, copy `data/sample-courses.txt` to `data/courses.txt`
and choose **Load from File**.

## 7. Limitations and Future Work

The system is single-user, uses a plain text file instead of a database, and
does not include authentication. The required seven actions do not include
editing or deleting a course, so those features are absent. Course codes follow
the project-specific three-letters-plus-three-digits assumption. Data is not
loaded automatically at startup. Future work could add edit/delete actions,
optional auto-loading, and automated UI tests.

## 8. Conclusion

The mini-system meets the required functionality with a small, understandable
design. Validation is centralized, search is genuinely recursive, totals use a
loop, and file operations avoid damaging existing data on failure. The project
builds successfully and passes all 56 automated checks.
