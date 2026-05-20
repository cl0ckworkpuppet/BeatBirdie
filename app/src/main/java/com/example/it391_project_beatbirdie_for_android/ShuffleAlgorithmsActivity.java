package com.example.it391_project_beatbirdie_for_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ShuffleAlgorithmsActivity extends AppCompatActivity {

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shuffle_algorithms);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Shuffle Algorithms");
        }

        // creating the recyclerview
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ShuffleAlgorithm> algList = new ArrayList<>();
        algList.add(new ShuffleAlgorithm("*Play in Order", "Plays songs in order without shuffling your music."));
        algList.add(new ShuffleAlgorithm("Fisher-Yates", "Walks through your music list and swaps each song with another song, chosen randomly."));
        algList.add(new ShuffleAlgorithm("True Random, No Repeats", "Gives each song a random spot in line, and then plays them in that order."));
        algList.add(new ShuffleAlgorithm("Fair Play", "Ensures a balanced variety by preventing the same artist or album from playing too frequently in a row."));
        algList.add(new ShuffleAlgorithm("Sattolo's Algorithm", "A variation of Fisher-Yates that ensures no song stays in its original position."));
        algList.add(new ShuffleAlgorithm("Casino Shuffle", "Simulates a riffle shuffle by splitting the playlist and interleaving the halves."));
        algList.add(new ShuffleAlgorithm("Prime-Step", "Traverses the list using a step size that is coprime with the playlist size, creating a unique cyclic pattern."));
        algList.add(new ShuffleAlgorithm("*Bit-Reversal", "Reorders songs based on the binary reversal of their indices, providing a structured yet scrambled feel."));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("experimental_shuffle_enabled", false)) {
            algList.add(new ShuffleAlgorithm("EXPERIMENTAL SHUFFLE", "An intentionally terrible shuffle algorithm. Use at your own risk."));
        }

        ShuffleAlgorithmAdapter adapter = new ShuffleAlgorithmAdapter(algList, this);
        recyclerView.setAdapter(adapter);
    }
}
