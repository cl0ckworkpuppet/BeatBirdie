package com.example.it391_project_beatbirdie_for_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShuffleAlgorithmAdapter extends RecyclerView.Adapter<ShuffleAlgorithmAdapter.ViewHolder> {

    // list of songs that will be displayed in the app
    private List<ShuffleAlgorithm> algs;
    private final AppCompatActivity activity;

    // constructor
    public ShuffleAlgorithmAdapter(List<ShuffleAlgorithm> algs, AppCompatActivity activity) {
        this.algs = algs;
        this.activity = activity;
    }

    // what is displayed in the viewbox
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.algName);
            description = view.findViewById(R.id.algDescription);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shuffle_alg_box, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShuffleAlgorithm algorithm = algs.get(position);

        holder.name.setText(algorithm.name);
        holder.description.setText(algorithm.description);

        holder.itemView.setOnClickListener(v -> {
            PlaybackHandler.setAlg(algorithm.name);
            notifyDataSetChanged();
            activity.finish();
        });
    }

    @Override
    public int getItemCount() {
        return algs.size();
    }
}