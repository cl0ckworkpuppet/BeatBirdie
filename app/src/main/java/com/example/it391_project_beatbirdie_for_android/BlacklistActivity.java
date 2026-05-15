package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlacklistActivity extends AppCompatActivity {

    private BlacklistAdapter adapter;
    private List<String> blacklistedPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blacklist);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.blacklistRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadBlacklist();
        adapter = new BlacklistAdapter(blacklistedPaths);
        recyclerView.setAdapter(adapter);
    }

    private void loadBlacklist() {
        Set<String> set = BlacklistManager.getBlacklist(this);
        blacklistedPaths = new ArrayList<>(set);
    }

    private class BlacklistAdapter extends RecyclerView.Adapter<BlacklistAdapter.ViewHolder> {
        private List<String> paths;

        public BlacklistAdapter(List<String> paths) {
            this.paths = paths;
        }

        public void updateData(List<String> newPaths) {
            this.paths = newPaths;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blacklisted_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String path = paths.get(position);
            holder.pathText.setText(path);
            
            File file = new File(path);
            holder.fileNameText.setText(file.getName());

            holder.btnRestore.setOnClickListener(v -> {
                BlacklistManager.remove(BlacklistActivity.this, path);
                loadBlacklist();
                updateData(blacklistedPaths);
                Toast.makeText(BlacklistActivity.this, "Restored " + file.getName(), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return paths.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView pathText, fileNameText;
            Button btnRestore;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                pathText = itemView.findViewById(R.id.blacklistedPath);
                fileNameText = itemView.findViewById(R.id.blacklistedFileName);
                btnRestore = itemView.findViewById(R.id.btnRestore);
            }
        }
    }
}
