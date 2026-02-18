package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;

public class NowPlayingActivity extends AppCompatActivity {

    // functionality for back button on the toolbar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_now_playing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // creating the toolbar onscreen
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // functionality for buttons
        /////////////////////////
        // play/pause
        ImageButton playPause = findViewById(R.id.playpause);
        playPause.setOnClickListener(v -> {
            // placeholder
            // TODO: add functionality to play/pause button
            Toast.makeText(this, "Play/Pause clicked", Toast.LENGTH_SHORT).show();
        });

        // skip
        ImageButton skipButton = findViewById(R.id.skip);
        skipButton.setOnClickListener(v -> {
            // placeholder
            // TODO: add functionality to skip button
            Toast.makeText(this, "Skip button clicked", Toast.LENGTH_SHORT).show();
        });

        // rewind
        ImageButton rewindButton = findViewById(R.id.rewind);
        rewindButton.setOnClickListener(v -> {
            // placeholder
            // TODO: add functionality to rewind button
            Toast.makeText(this, "Rewind button clicked", Toast.LENGTH_SHORT).show();
        });
    }
}