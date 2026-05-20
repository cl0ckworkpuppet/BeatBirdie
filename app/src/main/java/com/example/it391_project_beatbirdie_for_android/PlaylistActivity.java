package com.example.it391_project_beatbirdie_for_android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {

    private RecyclerView rvPlaylists;
    private android.widget.TextView tvNoPlaylists;
    private PlaylistAdapter adapter;
    private List<Playlist> playlistList;
    private AppDatabase db;
    private Playlist currentPlaylistToEdit;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentPlaylistToEdit != null) {
                    saveCustomThumbnail(uri, currentPlaylistToEdit);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlists);

        db = AppDatabase.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.playlistRoot), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvPlaylists = findViewById(R.id.rvPlaylists);
        tvNoPlaylists = findViewById(R.id.tvNoPlaylists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));

        loadPlaylists();

        ImageButton btnAddPlaylist = findViewById(R.id.btnAddPlaylist);
        btnAddPlaylist.setOnClickListener(v -> showAddPlaylistDialog());
    }

    private void showAddPlaylistDialog() {
        EditText input = new EditText(this);
        input.setHint("Playlist Name");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("New Playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Playlist newPlaylist = new Playlist(name);
                        db.playlistDao().insert(newPlaylist);
                        loadPlaylists();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPlaylists() {
        playlistList = db.playlistDao().getAllPlaylists();
        
        if (playlistList.isEmpty()) {
            tvNoPlaylists.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);
        } else {
            tvNoPlaylists.setVisibility(View.GONE);
            rvPlaylists.setVisibility(View.VISIBLE);
        }

        adapter = new PlaylistAdapter(playlistList, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                Intent intent = new Intent(PlaylistActivity.this, PlaylistDetailActivity.class);
                intent.putExtra("playlist_id", playlist.getId());
                intent.putExtra("playlist_name", playlist.getName());
                startActivity(intent);
            }

            @Override
            public void onPlaylistLongClick(Playlist playlist, View view) {
                showPlaylistMenu(playlist, view);
            }
        });
        rvPlaylists.setAdapter(adapter);
    }

    private void showPlaylistMenu(Playlist playlist, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Change Thumbnail");
        if (playlist.getCustomThumbnailPath() != null) {
            popup.getMenu().add("Remove Thumbnail");
        }
        popup.getMenu().add("Rename");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Change Thumbnail")) {
                currentPlaylistToEdit = playlist;
                galleryLauncher.launch("image/*");
                return true;
            } else if (item.getTitle().equals("Remove Thumbnail")) {
                removeThumbnail(playlist);
                return true;
            } else if (item.getTitle().equals("Rename")) {
                showRenameDialog(playlist);
                return true;
            } else if (item.getTitle().equals("Delete")) {
                confirmDeletePlaylist(playlist);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void removeThumbnail(Playlist playlist) {
        if (playlist.getCustomThumbnailPath() != null) {
            File thumbFile = new File(playlist.getCustomThumbnailPath());
            if (thumbFile.exists()) {
                thumbFile.delete();
            }
            playlist.setCustomThumbnailPath(null);
            db.playlistDao().update(playlist);
            loadPlaylists();
            Toast.makeText(this, "Thumbnail removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(Playlist playlist) {
        EditText input = new EditText(this);
        input.setText(playlist.getName());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Rename Playlist")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        playlist.setName(newName);
                        db.playlistDao().update(playlist);
                        loadPlaylists();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeletePlaylist(Playlist playlist) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete \"" + playlist.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the thumbnail file if it exists
                    if (playlist.getCustomThumbnailPath() != null) {
                        File thumbFile = new File(playlist.getCustomThumbnailPath());
                        if (thumbFile.exists()) {
                            thumbFile.delete();
                        }
                    }
                    db.playlistDao().deleteSongsByPlaylistId(playlist.getId());
                    db.playlistDao().delete(playlist);
                    loadPlaylists();
                    Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCustomThumbnail(Uri uri, Playlist playlist) {
        try {
            File thumbDir = new File(getFilesDir(), "playlist_thumbs");
            if (!thumbDir.exists()) thumbDir.mkdirs();

            // Delete old thumbnail if it exists to clear space and avoid caching issues
            if (playlist.getCustomThumbnailPath() != null) {
                File oldFile = new File(playlist.getCustomThumbnailPath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            // Use a unique filename with timestamp to force Glide to refresh its cache
            String fileName = "thumb_" + playlist.getId() + "_" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(thumbDir, fileName);
            InputStream is = getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            os.close();
            is.close();

            playlist.setCustomThumbnailPath(outFile.getAbsolutePath());
            db.playlistDao().update(playlist);
            loadPlaylists();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyTheme() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String themeValue = prefs.getString("dark_mode", "default");
        switch (themeValue) {
            case "light":
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "default":
            default:
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
