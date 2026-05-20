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
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import me.zhanghai.android.fastscroll.FastScrollerBuilder;

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
    private androidx.appcompat.widget.SearchView searchView;

    @Override
    public void onSongChanged() {
        runOnUiThread(this::updateNowPlayingBar);
    }

    @Override
    public void onQueueModified() {
        runOnUiThread(this::updateNowPlayingBar);
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int minDuration = Integer.parseInt(prefs.getString("filter_duration", "0"));

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
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
                int duration = cursor.getInt(6);
                String path = cursor.getString(7);

                // Filter out songs that are too short or blacklisted
                if (duration < minDuration || BlacklistManager.isBlacklisted(this, path)) {
                    continue;
                }

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
                songList.add(new Song(title, artist, album, contentUri, path, albumId, duration));
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
            adapter.updateList(songList);
        }
        // Update the playback handler's internal playlist to match the filtered/sorted list
        PlaybackHandler.updatePlaylist(songList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_playlists) {
            startActivity(new Intent(this, PlaylistActivity.class));
            return true;
        }
        else if (id == R.id.action_sort_title) {
            songList.sort((s1, s2) -> {
                String t1 = s1.getTitle();
                String t2 = s2.getTitle();
                if (t1 == null) t1 = "";
                if (t2 == null) t2 = "";
                int g1 = getGroup(t1);
                int g2 = getGroup(t2);
                if (g1 != g2) return Integer.compare(g1, g2);
                return t1.compareToIgnoreCase(t2);
            });
            PlaybackHandler.updatePlaylist(songList);
            if (adapter != null) {
                adapter.setSortType("title");
                adapter.updateList(songList);
                if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                    adapter.filter(searchView.getQuery().toString());
                }
            }
            return true;
        }
        else if (id == R.id.action_sort_artist) {
            songList.sort((s1, s2) -> {
                String a1 = s1.getArtist();
                String a2 = s2.getArtist();
                if (a1 == null) a1 = "";
                if (a2 == null) a2 = "";
                int g1 = getGroup(a1);
                int g2 = getGroup(a2);
                if (g1 != g2) return Integer.compare(g1, g2);
                return a1.compareToIgnoreCase(a2);
            });
            PlaybackHandler.updatePlaylist(songList);
            if (adapter != null) {
                adapter.setSortType("artist");
                adapter.updateList(songList);
                if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                    adapter.filter(searchView.getQuery().toString());
                }
            }
            return true;
        }
        else if (id == R.id.action_sort_album) {
            songList.sort((s1, s2) -> {
                String al1 = s1.getAlbum();
                String al2 = s2.getAlbum();
                if (al1 == null) al1 = "";
                if (al2 == null) al2 = "";
                int g1 = getGroup(al1);
                int g2 = getGroup(al2);
                if (g1 != g2) return Integer.compare(g1, g2);
                return al1.compareToIgnoreCase(al2);
            });
            PlaybackHandler.updatePlaylist(songList);
            if (adapter != null) {
                adapter.setSortType("album");
                adapter.updateList(songList);
                if (searchView != null && !searchView.getQuery().toString().isEmpty()) {
                    adapter.filter(searchView.getQuery().toString());
                }
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
            
            // Robust loading using custom AudioCoverModel.
            // This prioritizes embedded art via MediaMetadataRetriever (cached by Glide).
            // If embedded art fails, it falls back to the MediaStore album art URI.
            Glide.with(this)
                .load(new AudioCoverModel(currentSong.getUri()))
                .placeholder(R.drawable.ic_music_note)
                .error(Glide.with(this)
                    .load(currentSong.getAlbumArtUri())
                    .error(R.drawable.ic_music_note))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(nowPlayingCover);
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

    private void showBlacklistDialog(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Blacklist Song")
                .setMessage("Are you sure you want to blacklist \"" + song.getTitle() + "\"? It will be removed from your library.")
                .setPositiveButton("Blacklist", (dialog, which) -> {
                    BlacklistManager.add(this, song.getPath());
                    loadSongs(); // Refresh the list
                    Toast.makeText(this, "Song blacklisted. Manage in Settings.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private void applyTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themeValue = prefs.getString("dark_mode", "default");
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "default":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
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
        searchView = findViewById(R.id.searchView);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            // If the keyboard is dismissed and search is open but empty, close the search bar too
            if (!imeVisible && searchView != null && !searchView.isIconified() && searchView.getQuery().length() == 0) {
                searchView.setIconified(true);
                searchView.clearFocus();
            }
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup the list display
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongAdapter(songList, position -> {
            // Use the adapter's current list (which might be filtered)
            List<Song> currentList = adapter.getSongs();
            if (position >= 0 && position < currentList.size()) {
                PlaybackHandler.playSong(MainActivity.this, currentList, position);
                updateNowPlayingBar();
            }
        }, position -> {
            List<Song> currentList = adapter.getSongs();
            if (position >= 0 && position < currentList.size()) {
                showBlacklistDialog(currentList.get(position));
            }
        });
        recyclerView.setAdapter(adapter);

        new FastScrollerBuilder(recyclerView)
                .useMd2Style()
                .build();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_preferences);

        toolbar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        // Handle back gesture for search: collapse the bar immediately
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchView != null && !searchView.isIconified()) {
                    // Calling setIconified(true) twice is a reliable way to clear text AND collapse
                    searchView.setIconified(true);
                    searchView.setIconified(true);
                    searchView.clearFocus();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
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
