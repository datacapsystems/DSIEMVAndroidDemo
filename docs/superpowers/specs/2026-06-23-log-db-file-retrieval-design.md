# Log & DB File Retrieval — Test App Integration

**Date:** 2026-06-23
**Status:** Approved (design)
**Scope:** Demo/test app (`DSIEMVAndroidDemo`) only. Library-side work (the AAR's
`GetLogFiles()`, `GetDBFiles()`, `DatacapFile` model, `GetActiveLogFileName` JNI,
and Kotlin wrappers) is already complete and shipped in `dsiEMVAndroid/DSIEMVAndroid.aar`.

## Goal

Demonstrate and verify the new diagnostic-file retrieval API from the test app:

- Retrieve all log files via `GetLogFiles()` and all DB files via `GetDBFiles()`.
- Surface each returned `DatacapFile` with its `active` / `deletable` flags and size.
- Let the user **share/export** any file off the device (proving `getFile()` returns a
  readable `java.io.File`).
- Let the user **delete** any file, attempting `file.delete()` **unconditionally** —
  including files flagged `isDeletable() == false` and the active log — and report the
  actual OS result.

This satisfies the ticket's test steps: "Add new methods to the test app" and "Verify the
methods are able to retrieve the files (logs and databases)."

## API surface being consumed (already in the AAR — verified via `javap`)

```
com.datacap.android.dsiEMVAndroid
  public java.util.List<com.datacap.android.DatacapFile> GetLogFiles();
  public java.util.List<com.datacap.android.DatacapFile> GetDBFiles();

com.datacap.android.DatacapFile
  public java.io.File getFile();
  public boolean isActive();
  public boolean isDeletable();
```

`GetLogFiles()` / `GetDBFiles()` return an empty list (never null) when called before
initialization or when the directory does not exist. Both run on the library's serialized
executor internally.

## Architecture

The test app is Java. It talks to the library through the existing
`dsiEMVAndroidinstance.getInstance(context)` singleton. The new feature follows the
established UI pattern in `MainActivity` (buttons wired in `onCreate` via
`viewPager.post(...)`, work submitted to the single-thread `executor`, results posted back
to the UI via `handler`).

### Components

1. **Buttons (`trancode_card.xml`, `strings.xml`)**
   - Add one new horizontal button-bar row to the existing card with two buttons:
     - `@+id/getLogFilesButton` → text `@string/get_log_files` ("Get Log Files")
     - `@+id/getDBFilesButton`  → text `@string/get_db_files` ("Get DB Files")
   - Styling matches the existing `?android:attr/buttonBarButtonStyle` buttons.

2. **`DatacapFileListDialog.java` (new, self-contained unit)**
   - Responsibility: render a `List<DatacapFile>` in an `AlertDialog` backed by a
     `RecyclerView`, with per-row Share and Delete actions. No knowledge of the library
     beyond the `DatacapFile` model.
   - Public entry point: `static void show(Context ctx, String title, List<DatacapFile> files)`.
   - Inner `RecyclerView.Adapter` binds each row from `datacap_file_row.xml`.
   - **Share action:** builds a `content://` URI with
     `FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file.getFile())`,
     then launches an `ACTION_SEND` chooser with `FLAG_GRANT_READ_URI_PERMISSION` and an
     appropriate MIME type (`text/plain` for logs, `application/octet-stream` for DB files;
     `*/*` fallback).
   - **Delete action:** calls `file.getFile().delete()` **unconditionally** (the
     `isDeletable` flag is displayed but NOT used to gate the attempt), Toasts the boolean
     result (e.g. `"Delete <name>: true/false"`), and refreshes the list so the row's
     presence/flags reflect reality.

3. **`datacap_file_row.xml` (new layout)**
   - One row: a `TextView` showing `name`, `active=<bool>`, `deletable=<bool>`, and a
     human-readable size; a **Share** button; a **Delete** button.

4. **`MainActivity.java` wiring**
   - Two new `viewPager.post(...)` blocks (mirroring the existing button handlers):
     - On `getLogFilesButton` tap → `executor.submit(() -> { List<DatacapFile> files =
       dsiEMVAndroidinstance.getInstance(this).GetLogFiles(); handler.post(() ->
       DatacapFileListDialog.show(this, "Log Files", files)); })`.
     - On `getDBFilesButton` tap → same with `GetDBFiles()` and title "DB Files".
   - Empty list → Toast "No files found" instead of an empty dialog.
   - Add the two new R.id static imports alongside the existing ones.

5. **FileProvider (`AndroidManifest.xml`, `res/xml/file_paths.xml`)**
   - The files live under `context.getFilesDir()/DATACAP_LOGS` and `/DATACAP_DB`. Sharing
     internal-storage files requires a `FileProvider`.
   - Add a `<provider>` inside `<application>`:
     - `android:name="androidx.core.content.FileProvider"`
     - `android:authorities="${applicationId}.fileprovider"`
       (applicationId is `com.example.dsiemvandroiddemo`)
     - `android:exported="false"`, `android:grantUriPermissions="true"`
     - `<meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />`
   - `res/xml/file_paths.xml` exposes the two subdirectories with `<files-path>` entries
     (path `DATACAP_LOGS/` and `DATACAP_DB/`, or a single root `files-path` if the dir
     names are not guaranteed stable).

## Data flow

```
[Get Log Files] tap
  -> executor: GetLogFiles()  (List<DatacapFile>, possibly empty)
  -> handler (UI): empty? Toast "No files found"
                   else DatacapFileListDialog.show("Log Files", files)
       -> per row: name + active/deletable + size
            -> Share  -> FileProvider URI -> ACTION_SEND chooser
            -> Delete -> file.getFile().delete() (unconditional) -> Toast(result) -> refresh
```

`Get DB Files` is identical with `GetDBFiles()` and title "DB Files".

## Error handling

- `GetLogFiles()` / `GetDBFiles()` returning empty → friendly Toast, no dialog.
- Share: wrap `FileProvider.getUriForFile` / `startActivity` in try/catch; on failure Toast
  the error (e.g. file not under a configured FileProvider path, or no app to handle SEND).
- Delete: `file.delete()` returning `false` is an expected outcome for the active log /
  undeletable files — it is reported, not treated as a crash. Any thrown
  `SecurityException` is caught and Toasted.

## Testing / verification

Manual, per the ticket's test steps (no device-coupled automated tests in this app):

1. Build the app (`gradlew assembleDebug`) — confirms the new API compiles against the AAR.
2. Run on a device that has exercised the library (so `DATACAP_LOGS` / `DATACAP_DB` exist).
3. Tap **Get Log Files** → dialog lists log files; exactly one is `active=true,
   deletable=false`; the rest `deletable=true`.
4. Tap **Get DB Files** → dialog lists DB files, all `active=false, deletable=true`.
5. **Share** a file → chooser appears; export to email/Drive and confirm the file opens
   off-device (proves `getFile()` is readable/copyable).
6. **Delete** an inactive file → Toast `true`; re-open dialog → file gone.
7. **Delete** the active log → Toast reports the real OS result (the attempt is made
   regardless of the `deletable=false` flag).
8. Before initialization / empty dirs → Toast "No files found".

## Out of scope

- No remote-listener (`LocalListenerService`) route — these methods return `File`/list
  objects that do not serialize cleanly over the HTTP listener, and the ticket's test steps
  are on-device. (Can be added later if needed.)
- No changes to the library/AAR — already shipped.
- No new automated test harness.
