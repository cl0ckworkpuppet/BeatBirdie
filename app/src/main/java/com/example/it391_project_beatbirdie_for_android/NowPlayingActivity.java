package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import java.util.Arrays;
import java.util.List;

public class NowPlayingActivity extends AppCompatActivity {

    private ImageButton playPause;
    private TextView songTitle;
    private TextView artistAlbum;
    private Spinner shuffleSpinner;
    private boolean isInitialSelection = true;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void updateUI() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong != null) {
            String title = currentSong.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Unknown Title";
            }
            songTitle.setText(title);

            String artist = currentSong.getArtist();
            if (artist == null || artist.isEmpty()) {
                artist = "Unknown Artist";
            }

            String album = currentSong.getAlbum();
            if (album == null || album.isEmpty()) {
                album = "Unknown Album";
            }

            String details = artist + " - " + album;
            artistAlbum.setText(details);
        }
        else {
            songTitle.setText("No Song Playing");
            artistAlbum.setText("");
        }
        updatePlayPauseIcon();
        updateShuffleSpinnerSelection();
    }

    private void updatePlayPauseIcon() {
        if (PlaybackHandler.isPlaying()) {
            playPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void updateShuffleSpinnerSelection() {
        String currentAlg = PlaybackHandler.currentAlg();
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) shuffleSpinner.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(currentAlg);
            if (position != -1) {
                shuffleSpinner.setSelection(position, false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_now_playing);
        
        songTitle = findViewById(R.id.songTitle);
        artistAlbum = findViewById(R.id.artistAlbum);
        playPause = findViewById(R.id.playpause);
        ImageButton skipButton = findViewById(R.id.skip);
        ImageButton rewindButton = findViewById(R.id.rewind);
        shuffleSpinner = findViewById(R.id.shuffleSpinner);
        Button btnViewQueue = findViewById(R.id.btnViewQueue);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        setupShuffleSpinner();

        playPause.setOnClickListener(v -> {
            PlaybackHandler.toggle();
            updatePlayPauseIcon();
        });

        skipButton.setOnClickListener(v -> {
            PlaybackHandler.next(this);
            updateUI();
        });

        rewindButton.setOnClickListener(v -> {
            PlaybackHandler.previous(this);
            updateUI();
        });

        btnViewQueue.setOnClickListener(v -> showQueueDialog());
        
        updateUI();
    }

    private void setupShuffleSpinner() {
        List<String> algorithms = Arrays.asList("Default", "Fisher-Yates", "True Random, No Repeats", "Fair Play");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, algorithms);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shuffleSpinner.setAdapter(adapter);

        shuffleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialSelection) {
                    isInitialSelection = false;
                    return;
                }
                String selectedAlg = (String) parent.getItemAtPosition(position);
                PlaybackHandler.setAlg(selectedAlg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void showQueueDialog() {
        List<String> queue = PlaybackHandler.getQueueTitles();
        if (queue.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Queue")
                    .setMessage("The queue is empty.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] items = queue.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Current Playback Queue")
                .setItems(items, null)
                .setPositiveButton("Close", null)
                .show();
    }
}