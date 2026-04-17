package com.example.it391_project_beatbirdie_for_android;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity that displays the music library and a persistent play bar.
 * Handles permission requests and scans the device for audio files.
 */
public class MainActivity extends AppCompatActivity implements PlaybackHandler.PlaybackListener {
    private static final int REQUEST_PERMISSION_CODE = 101;
    private static final String TAG = "MainActivity";
    
    private ImageButton playPause;
    private TextView nowPlayingTitle;
    private TextView nowPlayingArtist;
    private ImageView nowPlayingCover;
    private LinearLayout nowPlayingBar;
    private final List<Song> songList = new ArrayList<>();
    private SongAdapter adapter;

    @Override
    public void onSongChanged() {
        runOnUiThread(this::updateNowPlayingBar);
    }

    @Override
    public void onQueueModified() {
        runOnUiThread(this::updateNowPlayingBar);
    }

    /**
     * Checks for appropriate storage permissions based on Android version.
     * Android 13+ uses READ_MEDIA_AUDIO, while older versions use READ_EXTERNAL_STORAGE.
     */
    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Explains to the user why the app needs file access before showing the system prompt.
     */
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Full File Access Required")
                .setMessage("This app needs access to your audio files to display and play your music library.")
                .setPositiveButton("Allow Access", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION_CODE);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                    }
                })
                .setNegativeButton("No Access", (dialog, which) -> Toast.makeText(MainActivity.this, "Note: Songs won't be visible without file access.", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs(); // Load songs immediately once permission is granted
                Toast.makeText(this, "Permission granted! Refreshing library...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Songs will not be visible.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // helper method for song sorting
    private int getGroup(String s) {
        if (s.isEmpty()) return 3;

        char c = s.charAt(0);

        if (Character.isLetter(c)) return 0;   // A–Z first
        if (Character.isDigit(c)) return 1;    // numbers next
        if (isAsciiSymbol(c)) return 2;        // [ ] ! etc.
        return 3;                              // everything else (「」 etc.)
    }

    private boolean isAsciiSymbol(char c) {
        return (c >= 33 && c <= 47) ||
                (c >= 58 && c <= 64) ||
                (c >= 91 && c <= 96) ||
                (c >= 123 && c <= 126);
    }

    /**
     * Scans the MediaStore database for all audio files on the device.
     * Updates the songList and notifies the adapter of changes.
     */
    private void loadSongs() {
        songList.clear();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM_ID
        };

        // mediastore sorting is case sensitive. removed params and replaced with null when appropriate
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            Log.d(TAG, "Query returned " + cursor.getCount() + " files.");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                String fileName = cursor.getString(4);
                long albumId = cursor.getLong(5);

                // Fallback to filename if metadata is missing
                if (title == null || title.isEmpty()) {
                    title = fileName;
                }
                if (artist == null || artist.isEmpty() || artist.equals("<unknown>")) {
                    artist = "Unknown Artist";
                }
                if (album == null || album.isEmpty() || album.equals("<unknown>") || album.equalsIgnoreCase("download")) {
                    album = "Unknown Album";
                }

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                );
                songList.add(new Song(title, artist, album, contentUri, albumId));
            }
            cursor.close();
        }

        // fixed line to sort by title regardless of case
        songList.sort((s1, s2) -> {
            String t1 = s1.getTitle();
            String t2 = s2.getTitle();

            if (t1 == null) t1 = "";
            if (t2 == null) t2 = "";

            int g1 = getGroup(t1);
            int g2 = getGroup(t2);

            // First: compare by group
            if (g1 != g2) return Integer.compare(g1, g2);

            // Then: normal case-insensitive compare
            return t1.compareToIgnoreCase(t2);
        });

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadSongs(); // Manually re-scan the library
            Toast.makeText(this, "Library scanned.", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.action_sort_title) {
            songList.sort((s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return true;
        }
        else if (id == R.id.action_sort_artist) {
            songList.sort((s1, s2) -> s1.getArtist().compareToIgnoreCase(s2.getArtist()));
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Syncs the bottom play bar UI with the current state of PlaybackHandler.
     */
    private void updateNowPlayingBar() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong != null) {
            nowPlayingBar.setVisibility(View.VISIBLE);
            nowPlayingTitle.setText(currentSong.getTitle());
            nowPlayingArtist.setText(currentSong.getArtist());
            // Prefer embedded artwork first, then MediaStore album art
            boolean loaded = false;
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(this, currentSong.getUri());
                byte[] art = mmr.getEmbeddedPicture();
                if (art != null && art.length > 0) {
                    Glide.with(this)
                        .asBitmap()
                        .load(art)
                        .placeholder(R.drawable.ic_music_note)
                        .into(nowPlayingCover);
                    loaded = true;
                }
            } catch (Exception e) {
                Log.w("MainActivity", "Failed to load embedded art", e);
            } finally {
                try { mmr.release(); } catch (Exception ignored) {}
            }

            if (!loaded) {
                Glide.with(this)
                    .load(currentSong.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note)
                    .into(nowPlayingCover);
            }
        } else {
            nowPlayingBar.setVisibility(View.GONE);
        }

        // Update play/pause icon based on whether music is active
        if (PlaybackHandler.isPlaying()) {
            playPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void applyKeepScreenOn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keepOn = prefs.getBoolean("lock_screen_on", false);
        View root = findViewById(R.id.coordinatorLayout);
        if (root != null) {
            root.setKeepScreenOn(keepOn);
        }
        
        // Also ensure window flags are synchronized
        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyKeepScreenOn();
        PlaybackHandler.addListener(this);
        // Refresh library and UI state whenever the user returns to this screen
        if (hasPermissions()) {
            loadSongs();
        }
        updateNowPlayingBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackHandler.removeListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize playback system
        PlaybackHandler.init(this);

        nowPlayingBar = findViewById(R.id.nowPlayingBar);
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle);
        nowPlayingArtist = findViewById(R.id.nowPlayingArtist);
        nowPlayingCover = findViewById(R.id.nowPlayingCover);
        playPause = findViewById(R.id.btnPlayPause);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup the list display
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongAdapter(songList, position -> {
            // When a song is clicked, start playing it
            PlaybackHandler.playSong(MainActivity.this, songList, position);
            updateNowPlayingBar();
        });
        recyclerView.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_preferences);

        toolbar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // Navigate to the full-screen player when bar is tapped
        nowPlayingBar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NowPlayingActivity.class);
            startActivity(intent);
        });

        // Simple toggle button on the main screen
        playPause.setOnClickListener(v -> {
            PlaybackHandler.toggle();
            updateNowPlayingBar();
        });

        // Ensure permissions are handled on first launch
        if (!hasPermissions()) {
            showPermissionRationaleDialog();
        }
        
        // Initial bar state
        updateNowPlayingBar();
    }
}
