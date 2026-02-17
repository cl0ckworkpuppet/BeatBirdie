package com.example.it391_project_beatbirdie_for_android;

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
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // initial variable for requesting access to local storage
    private static final int REQUEST_PERMISSION_CODE = 101;

    // function for checking whether or not permissions for file access have been granted.
    // if permissions already granted, return true.
    // if not yet, request.
    // this code was written with AI assistance.
    // specifically and especially with writing the contents of the nested if statements, and +
    // + what to do with the Compats.
    // please check and test appropriately.
    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSION_CODE);
                return false;
            }
        } else { // android 12-
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                return false;
            }
        }
        return true; // permissions granted
    }

    // function to handle permission request results.
    // UNFINISHED. to be expanded when other functions to actually access files are written
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // TODO: file access function calls to actually use these permissions.
            } else {
                // TODO: explain that a music player app needs access to your music to play your music
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinatorLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Song> songList = new ArrayList<>();
        // PLACEHOLDER VALUES. DO NOT KEEP
        songList.add(new Song("Dummy Song 1", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 2", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 3", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 4", "Guy", "Placeholder Album"));
        songList.add(new Song("Dummy Song 5", "Guy", "Placeholder Album"));

        SongAdapter adapter = new SongAdapter(songList);
        recyclerView.setAdapter(adapter);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
}