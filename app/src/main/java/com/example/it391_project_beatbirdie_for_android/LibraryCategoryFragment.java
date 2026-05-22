package com.example.it391_project_beatbirdie_for_android;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryCategoryFragment extends Fragment {

    private static final String ARG_CATEGORY_TYPE = "category_type";
    public static final String TYPE_ALBUMS = "Albums";
    public static final String TYPE_ARTISTS = "Artists";
    public static final String TYPE_GENRES = "Genres";

    private String categoryType;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private List<String> items = new ArrayList<>();
    // For simplicity, using a basic adapter here. In a real app, might want specialized models.
    private CategoryAdapter adapter;

    public static LibraryCategoryFragment newInstance(String categoryType) {
        LibraryCategoryFragment fragment = new LibraryCategoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_TYPE, categoryType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryType = getArguments().getString(ARG_CATEGORY_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_category, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        loadData();
        
        adapter = new CategoryAdapter(items, categoryType, value -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), CategoryDetailActivity.class);
            intent.putExtra("category_type", categoryType);
            intent.putExtra("category_value", value);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    private void loadData() {
        items.clear();
        Set<String> resultSet = new HashSet<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection;
        String sortOrder;

        switch (categoryType) {
            case TYPE_ALBUMS:
                projection = new String[]{MediaStore.Audio.Media.ALBUM};
                sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";
                break;
            case TYPE_ARTISTS:
                projection = new String[]{MediaStore.Audio.Media.ARTIST};
                sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";
                break;
            case TYPE_GENRES:
                // Genres are a bit more complex in MediaStore, but for this simplified version,
                // we'll try to get them if available or just use a placeholder if not implemented.
                // Note: Proper Genre querying usually requires MediaStore.Audio.Genres
                loadGenres(resultSet);
                items.addAll(resultSet);
                Collections.sort(items);
                updateEmptyView();
                return;
            default:
                return;
        }

        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int index = cursor.getColumnIndex(projection[0]);
                while (cursor.moveToNext()) {
                    String value = cursor.getString(index);
                    if (value != null && !value.isEmpty() && !value.equals("<unknown>")) {
                        resultSet.add(value);
                    }
                }
            }
        }
        
        items.addAll(resultSet);
        Collections.sort(items);
        updateEmptyView();
    }

    private void loadGenres(Set<String> resultSet) {
        Uri uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.Audio.Genres.NAME};
        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                int index = cursor.getColumnIndex(MediaStore.Audio.Genres.NAME);
                while (cursor.moveToNext()) {
                    String value = cursor.getString(index);
                    if (value != null && !value.isEmpty()) {
                        resultSet.add(value);
                    }
                }
            }
        }
    }

    private void updateEmptyView() {
        if (items.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No " + categoryType.toLowerCase() + " found");
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<String> data;
        private final String type;
        private final OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onCategoryClick(String value);
        }

        CategoryAdapter(List<String> data, String type, OnCategoryClickListener listener) {
            this.data = data;
            this.type = type;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String value = data.get(position);
            holder.textView.setText(value);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(value);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
