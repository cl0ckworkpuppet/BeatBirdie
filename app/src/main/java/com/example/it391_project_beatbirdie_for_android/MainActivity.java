package com.example.it391_project_beatbirdie_for_android;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

public class MainActivity extends AppCompatActivity {
    // initial variable for requesting access to local storage
    private static final int REQUEST_PERMISSION_CODE = 101;

    // imagebutton variable so the functions dont break lol
    private ImageButton playPause;

    // Checks if the required file permissions are granted based on Android version. Returns true if permissions are granted, false otherwise.
    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // android 13+
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else { // android 12-
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // Provides an option to allow access (triggers system prompt) or decline (shows disclaimer).
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Full File Access Required")
                .setMessage("This app needs access to your audio files to display and play your music library. Without this permission, your songs will not be visible.")
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

    // function to handle permission request results.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // TODO: file access function calls to actually use these permissions.
                Toast.makeText(this, "Permission granted! Refreshing library...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Songs will not be visible.", Toast.LENGTH_LONG).show();
            }
        }
    }


    // basically just allows the settings menu to be created
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // connects settings menu button to SettingsActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // updates the play/pause icon for the screen
    private void updatePlayPauseIcon(ImageButton button) {
        if (PlaybackHandler.isPaused()) {
            button.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            button.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    // allows for the updating of the play/pause button between screens
    // to accurately represent whether the song is paused or not
    @Override
    protected void onResume() {
        super.onResume();
        updatePlayPauseIcon(playPause);
    }

    /* this is the big important function that sets up everything in the app
    when it is launched. if you're adding something to the app, it's almost
    certainly going to involve putting something in here. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Check for permissions on app launch
        if (!hasPermissions()) {
            showPermissionRationaleDialog();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // creating the recyclerview
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // add songs to the list of songs
        List<Song> songList = new ArrayList<>();
        // PLACEHOLDER VALUES. DO NOT KEEP. REPLACE WITH PROPER USER FILES.
        // TODO: exchange dummy values for getting local files
        songList.add(new Song("Dummy Song 1", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 2", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 3", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 4", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 5", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 6", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 7", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 8", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 9", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 10", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 11", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 12", "Guy", "Placeholder Album"));

        // connect this list to the song display
        SongAdapter adapter = new SongAdapter(songList);
        recyclerView.setAdapter(adapter);

        // create toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // buttons for the now playing bar
        LinearLayout nowPlayingBar = findViewById(R.id.nowPlayingBar);
        playPause = findViewById(R.id.btnPlayPause);

        // send user to the Now Playing screen when they click the Now Playing bar
        nowPlayingBar.setOnClickListener(v -> {
            // see: NowPlayingActivity.java
            Intent intent = new Intent(MainActivity.this, NowPlayingActivity.class);
            startActivity(intent);
        });

        // quick button to play/pause on the Now Playing bar for convenience
        playPause.setOnClickListener(v -> {
            // TODO: add functionality to play/pause button

            // placeholder text
            Toast.makeText(this, "Play/Pause clicked", Toast.LENGTH_SHORT).show();

            // toggle playback
            PlaybackHandler.toggle();

            // update icon
            updatePlayPauseIcon(playPause);
        });
    }
}
