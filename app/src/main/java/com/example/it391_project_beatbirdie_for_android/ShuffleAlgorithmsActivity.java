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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        List<ShuffleAlgorithm> allAlgs = new ArrayList<>();
        allAlgs.add(new ShuffleAlgorithm("*Play in Order", "Plays songs in order without shuffling your music."));
        allAlgs.add(new ShuffleAlgorithm("Fisher-Yates", "Walks through your music list and swaps each song with another song, chosen randomly."));
        allAlgs.add(new ShuffleAlgorithm("True Random, No Repeats", "Gives each song a random spot in line, and then plays them in that order."));
        allAlgs.add(new ShuffleAlgorithm("Fair Play", "Ensures a balanced variety by preventing the same artist or album from playing too frequently in a row."));
        allAlgs.add(new ShuffleAlgorithm("Sattolo's Algorithm", "A variation of Fisher-Yates that ensures no song stays in its original position."));
        allAlgs.add(new ShuffleAlgorithm("Casino Shuffle", "Simulates a riffle shuffle by splitting the playlist and interleaving the halves."));
        allAlgs.add(new ShuffleAlgorithm("Prime-Step", "Traverses the list using a step size that is coprime with the playlist size, creating a unique cyclic pattern."));
        allAlgs.add(new ShuffleAlgorithm("*Bit-Reversal", "Reorders songs based on the binary reversal of their indices, providing a structured yet scrambled feel."));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("experimental_shuffle_enabled", false)) {
            allAlgs.add(new ShuffleAlgorithm("EXPERIMENTAL SHUFFLE", "An intentionally terrible shuffle algorithm. Use at your own risk."));
        }

        Set<String> favorites = prefs.getStringSet("favourite_shuffle_algorithms", new HashSet<>());
        List<ShuffleAlgorithm> favoriteAlgs = new ArrayList<>();
        List<ShuffleAlgorithm> otherAlgs = new ArrayList<>();

        for (ShuffleAlgorithm alg : allAlgs) {
            if (favorites.contains(alg.getName())) {
                favoriteAlgs.add(alg);
            } else {
                otherAlgs.add(alg);
            }
        }

        List<ShuffleAlgorithm> sortedAlgs = new ArrayList<>(favoriteAlgs);
        sortedAlgs.addAll(otherAlgs);

        ShuffleAlgorithmAdapter adapter = new ShuffleAlgorithmAdapter(sortedAlgs, this);
        recyclerView.setAdapter(adapter);
    }
}
