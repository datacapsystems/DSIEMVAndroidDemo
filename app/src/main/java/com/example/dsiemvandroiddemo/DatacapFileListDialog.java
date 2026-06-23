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
