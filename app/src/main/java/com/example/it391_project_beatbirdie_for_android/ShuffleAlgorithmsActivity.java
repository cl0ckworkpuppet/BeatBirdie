package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ShuffleAlgorithmsActivity extends AppCompatActivity {

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

        // creating the recyclerview
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<ShuffleAlgorithm> algList = new ArrayList<>();
        algList.add(new ShuffleAlgorithm("Fisher-Yates", "Walks through your music list and swaps each song with another song, chosen randomly."));
        algList.add(new ShuffleAlgorithm("True Random, No Repeats", "Gives each song a random spot in line, and then plays them in that order."));
        algList.add(new ShuffleAlgorithm("Fair Play", "Ensures a balanced variety by preventing the same artist or album from playing too frequently in a row."));

        ShuffleAlgorithmAdapter adapter = new ShuffleAlgorithmAdapter(algList, this);
        recyclerView.setAdapter(adapter);
    }
}