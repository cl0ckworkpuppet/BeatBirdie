package com.example.it391_project_beatbirdie_for_android;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
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
    private RecyclerView recyclerView;
    private final List<Song> songList = new ArrayList<>();
    private SongAdapter adapter;

    @Override
    public void onSongChanged() {
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
                .setNegativeButton("No Access", (dialog, which) -> {
                    Toast.makeText(MainActivity.this, "Note: Songs won't be visible without file access.", Toast.LENGTH_LONG).show();
                })
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
                MediaStore.Audio.Media.DISPLAY_NAME
        };

        // Query any audio files found in common storage locations
        Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Audio.Media.TITLE + " ASC");
        
        if (cursor != null) {
            Log.d(TAG, "Query returned " + cursor.getCount() + " files.");
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                String fileName = cursor.getString(4);
                
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

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                songList.add(new Song(title, artist, album, contentUri));
            }
            cursor.close();
        }
        
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
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            loadSongs(); // Manually re-scan the library
            return true;
        }
        else if (id == R.id.action_sort_title) {
            songList.sort((s1, s2) -> s1.getArtist().compareToIgnoreCase(s2.getTitle()));
            adapter.notifyDataSetChanged();
            return true;
        }
        else if (id == R.id.action_sort_artist) {
            songList.sort((s1,s2) -> s1.getArtist().compareToIgnoreCase(s2.getArtist()));
            adapter.notifyDataSetChanged();
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
            nowPlayingTitle.setText(currentSong.getTitle());
            nowPlayingArtist.setText(currentSong.getArtist());
        } else {
            nowPlayingTitle.setText("No Song Playing");
            nowPlayingArtist.setText("");
        }

        // Update play/pause icon based on whether music is active
        if (PlaybackHandler.isPlaying()) {
            playPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        nowPlayingTitle = findViewById(R.id.nowPlayingTitle);
        nowPlayingArtist = findViewById(R.id.nowPlayingArtist);
        playPause = findViewById(R.id.btnPlayPause);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup the list display
        recyclerView = findViewById(R.id.recyclerView);
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

        // Navigate to the full-screen player when bar is tapped
        LinearLayout nowPlayingBar = findViewById(R.id.nowPlayingBar);
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
    }
}
