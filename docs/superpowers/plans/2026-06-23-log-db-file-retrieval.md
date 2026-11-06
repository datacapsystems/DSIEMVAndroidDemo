# Log & DB File Retrieval — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add on-device UI to the test app to retrieve, share/export, and delete the library's log and DB files via the new `GetLogFiles()` / `GetDBFiles()` / `DatacapFile` API.

**Architecture:** Two new buttons in `trancode_card.xml` call `GetLogFiles()` / `GetDBFiles()` on the existing single-thread `executor`, then open a self-contained `DatacapFileListDialog` (RecyclerView) that lists each `DatacapFile` with its `active`/`deletable` flags and size, plus per-row **Share** (via `FileProvider` + `ACTION_SEND`) and **Delete** (unconditional `file.delete()`, reporting the OS result).

**Tech Stack:** Java, Android (minSdk 23 / compileSdk 34), AndroidX `recyclerview` 1.3.2 and `appcompat` 1.7.0 (both already deps), `androidx.core.content.FileProvider`. Build tool: Gradle wrapper (`./gradlew`).

**Testing note:** This app has no automated/unit test harness and the code is UI/device-coupled, so per the spec verification is **(a) a successful `assembleDebug` build** after each task (the automated gate) and **(b) a final manual on-device verification** (Task 6). This is an intentional deviation from TDD because there is no runnable test layer for this UI code.

**Branch:** Work happens on `feat/log-db-file-retrieval` (already created; the design spec is committed there).

---

### Task 1: Add string resources and the two buttons

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/trancode_card.xml`

- [ ] **Step 1: Add string resources**

Add these four `<string>` entries inside the `<resources>` element in `app/src/main/res/values/strings.xml`:

```xml
<string name="get_log_files">Get Log Files</string>
<string name="get_db_files">Get DB Files</string>
<string name="share">Share</string>
<string name="delete">Delete</string>
```

- [ ] **Step 2: Add the button row to the trancode card**

In `app/src/main/res/layout/trancode_card.xml`, add a new button-bar row immediately **after** the closing `</LinearLayout>` of the `padResetButton` / `collectCardButton` row (i.e. just before the final `</LinearLayout>` that closes the vertical container at line ~123). Match the existing row style:

```xml
<LinearLayout
    style="?android:attr/buttonBarStyle"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:weightSum="10">

    <Button
        android:id="@+id/getLogFilesButton"
        style="?android:attr/buttonBarButtonStyle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="5"
        android:text="@string/get_log_files" />

    <Button
        android:id="@+id/getDBFilesButton"
        style="?android:attr/buttonBarButtonStyle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="5"
        android:text="@string/get_db_files" />
</LinearLayout>
```

- [ ] **Step 3: Build to verify resources compile**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. (The new `R.id.getLogFilesButton` / `R.id.getDBFilesButton` and strings now resolve. They are not wired yet — that is Task 5.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/layout/trancode_card.xml
git commit -m "Add Get Log Files / Get DB Files buttons and strings"
```

---

### Task 2: Configure FileProvider for sharing internal files

**Files:**
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create the FileProvider path config**

Create `app/src/main/res/xml/file_paths.xml`. The library writes to `context.getFilesDir()/DATACAP_LOGS` and `/DATACAP_DB`; expose both:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <files-path name="datacap_logs" path="DATACAP_LOGS/" />
    <files-path name="datacap_db" path="DATACAP_DB/" />
</paths>
```

- [ ] **Step 2: Register the provider in the manifest**

In `app/src/main/AndroidManifest.xml`, add this `<provider>` inside the `<application>` element (e.g. right after the `<service ... LocalListenerService .../>` line):

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

The resulting authority is `com.example.dsiemvandroiddemo.fileprovider` (the app's `applicationId`). Task 4 references this same authority via `ctx.getPackageName() + ".fileprovider"`.

- [ ] **Step 3: Build to verify the manifest and resource are valid**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (no manifest-merger or resource errors).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/xml/file_paths.xml app/src/main/AndroidManifest.xml
git commit -m "Add FileProvider for sharing DATACAP_LOGS / DATACAP_DB files"
```

---

### Task 3: Create the file-row layout

**Files:**
- Create: `app/src/main/res/layout/datacap_file_row.xml`

- [ ] **Step 1: Create the row layout**

Create `app/src/main/res/layout/datacap_file_row.xml`. One row = file info text (weighted to fill) + Share + Delete buttons:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:padding="8dp">

    <TextView
        android:id="@+id/fileInfo"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textSize="12sp" />

    <Button
        android:id="@+id/shareButton"
        style="?android:attr/buttonBarButtonStyle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/share" />

    <Button
        android:id="@+id/deleteButton"
        style="?android:attr/buttonBarButtonStyle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/delete" />
</LinearLayout>
```

- [ ] **Step 2: Build to verify the layout compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. (`@string/share` and `@string/delete` were added in Task 1.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/datacap_file_row.xml
git commit -m "Add datacap_file_row layout for file list dialog"
```

---

### Task 4: Create the DatacapFileListDialog

**Files:**
- Create: `app/src/main/java/com/example/dsiemvandroiddemo/DatacapFileListDialog.java`

- [ ] **Step 1: Write the dialog class**

Create `app/src/main/java/com/example/dsiemvandroiddemo/DatacapFileListDialog.java` with the full content below. `show(...)` builds an `AlertDialog` hosting a `RecyclerView`; the adapter binds each `DatacapFile`, wires Share (FileProvider + `ACTION_SEND`) and Delete (unconditional `file.delete()`, Toasting the boolean result and removing the row on success).

```java
package com.example.dsiemvandroiddemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.datacap.android.DatacapFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Self-contained dialog that lists a List<DatacapFile> with per-row Share and Delete actions.
// Knows only about the DatacapFile model; the library call that produced the list happens in MainActivity.
public class DatacapFileListDialog
{
    public static void show(Context ctx, String title, List<DatacapFile> files)
    {
        RecyclerView rv = new RecyclerView(ctx);
        rv.setLayoutManager(new LinearLayoutManager(ctx));
        rv.setAdapter(new Adapter(ctx, new ArrayList<>(files)));

        new AlertDialog.Builder(ctx)
                .setTitle(title + " (" + files.size() + ")")
                .setView(rv)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.VH>
    {
        private final Context ctx;
        private final List<DatacapFile> items;

        Adapter(Context ctx, List<DatacapFile> items)
        {
            this.ctx = ctx;
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(ctx).inflate(R.layout.datacap_file_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position)
        {
            DatacapFile item = items.get(position);
            File f = item.getFile();
            holder.info.setText(String.format(
                    "%s%nactive=%b  deletable=%b  %s",
                    f.getName(), item.isActive(), item.isDeletable(), humanSize(f.length())));

            holder.shareButton.setOnClickListener(v -> share(item));

            holder.deleteButton.setOnClickListener(v ->
            {
                // Unconditional attempt: ignores isDeletable so undeletable files and the active log
                // are attempted too, and the real OS result is reported.
                boolean deleted = f.delete();
                Toast.makeText(ctx, "Delete " + f.getName() + ": " + deleted, Toast.LENGTH_SHORT).show();
                if (deleted)
                {
                    int idx = items.indexOf(item);
                    if (idx >= 0)
                    {
                        items.remove(idx);
                        notifyItemRemoved(idx);
                    }
                }
            });
        }

        @Override
        public int getItemCount()
        {
            return items.size();
        }

        private void share(DatacapFile item)
        {
            File f = item.getFile();
            try
            {
                Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", f);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mimeFor(f));
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(Intent.createChooser(send, "Share " + f.getName()));
            }
            catch (Exception ex)
            {
                Toast.makeText(ctx, "Share failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        private static String mimeFor(File f)
        {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".log") || name.endsWith(".txt"))
            {
                return "text/plain";
            }
            return "application/octet-stream";
        }

        private static String humanSize(long bytes)
        {
            if (bytes < 1024)
            {
                return bytes + " B";
            }
            if (bytes < 1024 * 1024)
            {
                return (bytes / 1024) + " KB";
            }
            return (bytes / (1024 * 1024)) + " MB";
        }

        static class VH extends RecyclerView.ViewHolder
        {
            final TextView info;
            final Button shareButton;
            final Button deleteButton;

            VH(@NonNull View itemView)
            {
                super(itemView);
                info = itemView.findViewById(R.id.fileInfo);
                shareButton = itemView.findViewById(R.id.shareButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify the class compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. This confirms `com.datacap.android.DatacapFile` and its `getFile()` / `isActive()` / `isDeletable()` resolve against the AAR, and that `R.layout.datacap_file_row` / `R.id.fileInfo|shareButton|deleteButton` resolve.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dsiemvandroiddemo/DatacapFileListDialog.java
git commit -m "Add DatacapFileListDialog with per-file share and delete"
```

---

### Task 5: Wire the buttons in MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/dsiemvandroiddemo/MainActivity.java`

- [ ] **Step 1: Add the R.id static imports**

In `MainActivity.java`, alongside the existing `import static com.example.dsiemvandroiddemo.R.id.*;` lines (the block around lines 52–67), add:

```java
import static com.example.dsiemvandroiddemo.R.id.getLogFilesButton;
import static com.example.dsiemvandroiddemo.R.id.getDBFilesButton;
```

- [ ] **Step 2: Add the button handlers in onCreate**

In `MainActivity.java`, add the following two `viewPager.post(...)` blocks immediately **after** the existing `collectCardButton` handler block (the one at lines ~420–423, `findViewById(collectCardButton).setOnClickListener((v) -> CollectCardData());`). `GetLogFiles()` / `GetDBFiles()` run on the existing `executor`; the dialog opens on the UI thread via `handler`. `List` is already imported (`java.util.List`):

```java
viewPager.post(() ->
{
    findViewById(getLogFilesButton).setOnClickListener((v) ->
        executor.submit(() ->
        {
            java.util.List<com.datacap.android.DatacapFile> files =
                    dsiEMVAndroidinstance.getInstance(MainActivity.this).GetLogFiles();
            handler.post(() ->
            {
                if (files.isEmpty())
                {
                    Toast.makeText(MainActivity.this, "No log files found", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    DatacapFileListDialog.show(MainActivity.this, "Log Files", files);
                }
            });
        }));
});

viewPager.post(() ->
{
    findViewById(getDBFilesButton).setOnClickListener((v) ->
        executor.submit(() ->
        {
            java.util.List<com.datacap.android.DatacapFile> files =
                    dsiEMVAndroidinstance.getInstance(MainActivity.this).GetDBFiles();
            handler.post(() ->
            {
                if (files.isEmpty())
                {
                    Toast.makeText(MainActivity.this, "No DB files found", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    DatacapFileListDialog.show(MainActivity.this, "DB Files", files);
                }
            });
        }));
});
```

(`Toast` is already imported in `MainActivity`. The fully-qualified `java.util.List` / `com.datacap.android.DatacapFile` avoid adding imports, matching the file's existing fully-qualified usages.)

- [ ] **Step 3: Build to verify wiring compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. The buttons now call the library and open the dialog.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dsiemvandroiddemo/MainActivity.java
git commit -m "Wire Get Log Files / Get DB Files buttons to library + dialog"
```

---

### Task 6: Manual on-device verification

**Files:** none (verification only)

This is the ticket's required test step ("Verify the methods are able to retrieve the files"). There is no automated harness for device-coupled UI, so this is performed by hand.

- [ ] **Step 1: Install the debug build on a real device**

Run: `./gradlew installDebug` (device connected via adb) or install the APK from
`app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 2: Generate library files (if none exist yet)**

Run any transaction flow (e.g. EMVParamDownload / Sale) so the library writes to
`DATACAP_LOGS/` and `DATACAP_DB/`. Without this the lists may legitimately be empty.

- [ ] **Step 3: Verify log retrieval**

Tap **Get Log Files**. Expected: dialog titled `Log Files (N)` listing log files. Exactly one
row shows `active=true  deletable=false`; all others `active=false  deletable=true`. Each row
shows a non-zero size.

- [ ] **Step 4: Verify DB retrieval**

Tap **Get DB Files**. Expected: dialog titled `DB Files (N)` listing DB files, all rows
`active=false  deletable=true`.

- [ ] **Step 5: Verify share/export**

Tap **Share** on any row. Expected: Android chooser appears; sending to e.g. Drive/email/
Files produces a readable copy off-device. This confirms `getFile()` returns a valid,
readable `java.io.File`.

- [ ] **Step 6: Verify delete behavior**

- Tap **Delete** on an inactive (`deletable=true`) file → Toast `Delete <name>: true`; the
  row disappears; re-opening the dialog confirms it is gone.
- Tap **Delete** on the active log (`deletable=false`) → Toast reports the real OS result
  (`true` or `false`); the attempt is made regardless of the flag. Record the observed result.

- [ ] **Step 7: Verify empty / pre-init handling**

On a fresh install before any library activity (or after deleting all files), tap each button
→ expect a "No log files found" / "No DB files found" Toast and no dialog.

- [ ] **Step 8: Record results**

Note the observed outcomes (especially the active-log delete result) in the PR description.

---

## Verification summary

| Spec requirement | Covered by |
|---|---|
| `GetLogFiles()` returns all `DATACAP_LOGS/` files | Task 5 (wiring) + Task 6 Step 3 |
| Active log marked `isActive=true, isDeletable=false`; others `deletable=true` | Task 6 Step 3 (display in Task 4) |
| `GetDBFiles()` returns all `DATACAP_DB/` files, all `deletable=true` | Task 5 + Task 6 Step 4 |
| Empty list before init / missing dirs | Task 5 (empty-Toast) + Task 6 Step 7 |
| `getFile()` returns a readable/copyable `File` (share/export) | Task 2 (FileProvider) + Task 4 (share) + Task 6 Step 5 |
| Delete attempted unconditionally incl. active log | Task 4 (delete) + Task 6 Step 6 |
| On-device only (no remote route) | Scope — no `LocalListenerService` change in any task |
