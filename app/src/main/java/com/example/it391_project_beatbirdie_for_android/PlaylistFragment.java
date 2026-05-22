package com.example.it391_project_beatbirdie_for_android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PlaylistFragment extends Fragment {

    private RecyclerView rvPlaylists;
    private android.widget.TextView tvNoPlaylists;
    private PlaylistAdapter adapter;
    private List<Playlist> playlistList;
    private AppDatabase db;
    private Playlist currentPlaylistToEdit;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentPlaylistToEdit != null) {
                    saveCustomThumbnail(uri, currentPlaylistToEdit);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        tvNoPlaylists = view.findViewById(R.id.tvNoPlaylists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        db = AppDatabase.getInstance(requireContext());
        loadPlaylists();
        return view;
    }

    public void showAddPlaylistDialog() {
        EditText input = new EditText(getContext());
        input.setHint("Playlist Name");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("New Playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Playlist newPlaylist = new Playlist(name);
                        db.playlistDao().insert(newPlaylist);
                        loadPlaylists();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPlaylists() {
        playlistList = db.playlistDao().getAllPlaylists();
        
        for (Playlist p : playlistList) {
            if (p.getCustomThumbnailPath() != null) {
                p.setThumbnailUri(android.net.Uri.fromFile(new File(p.getCustomThumbnailPath())));
            } else {
                List<String> paths = db.playlistDao().getSongPathsForPlaylist(p.getId());
                p.setThumbnailUri(Playlist.findEarliestSongWithCover(requireContext(), paths));
            }
        }
        
        if (playlistList.isEmpty()) {
            tvNoPlaylists.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);
        } else {
            tvNoPlaylists.setVisibility(View.GONE);
            rvPlaylists.setVisibility(View.VISIBLE);
        }

        adapter = new PlaylistAdapter(playlistList, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                Intent intent = new Intent(getActivity(), PlaylistDetailActivity.class);
                intent.putExtra("playlist_id", playlist.getId());
                intent.putExtra("playlist_name", playlist.getName());
                startActivity(intent);
            }

            @Override
            public void onPlaylistLongClick(Playlist playlist, View view) {
                showPlaylistMenu(playlist, view);
            }
        });
        rvPlaylists.setAdapter(adapter);
    }

    private void showPlaylistMenu(Playlist playlist, View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Change Thumbnail");
        if (playlist.getCustomThumbnailPath() != null) {
            popup.getMenu().add("Remove Thumbnail");
        }
        popup.getMenu().add("Rename");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Change Thumbnail")) {
                currentPlaylistToEdit = playlist;
                galleryLauncher.launch("image/*");
                return true;
            } else if (item.getTitle().equals("Remove Thumbnail")) {
                removeThumbnail(playlist);
                return true;
            } else if (item.getTitle().equals("Rename")) {
                showRenameDialog(playlist);
                return true;
            } else if (item.getTitle().equals("Delete")) {
                confirmDeletePlaylist(playlist);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void removeThumbnail(Playlist playlist) {
        if (playlist.getCustomThumbnailPath() != null) {
            File thumbFile = new File(playlist.getCustomThumbnailPath());
            if (thumbFile.exists()) {
                thumbFile.delete();
            }
            playlist.setCustomThumbnailPath(null);
            db.playlistDao().update(playlist);
            loadPlaylists();
            Toast.makeText(getContext(), "Thumbnail removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(Playlist playlist) {
        EditText input = new EditText(getContext());
        input.setText(playlist.getName());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("Rename Playlist")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        playlist.setName(newName);
                        db.playlistDao().update(playlist);
                        loadPlaylists();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeletePlaylist(Playlist playlist) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete \"" + playlist.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (playlist.getCustomThumbnailPath() != null) {
                        File thumbFile = new File(playlist.getCustomThumbnailPath());
                        if (thumbFile.exists()) {
                            thumbFile.delete();
                        }
                    }
                    db.playlistDao().deleteSongsByPlaylistId(playlist.getId());
                    db.playlistDao().delete(playlist);
                    loadPlaylists();
                    Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCustomThumbnail(Uri uri, Playlist playlist) {
        try {
            File thumbDir = new File(requireContext().getFilesDir(), "playlist_thumbs");
            if (!thumbDir.exists()) thumbDir.mkdirs();

            if (playlist.getCustomThumbnailPath() != null) {
                File oldFile = new File(playlist.getCustomThumbnailPath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            String fileName = "thumb_" + playlist.getId() + "_" + System.currentTimeMillis() + ".jpg";
            File outFile = new File(thumbDir, fileName);
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream os = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            os.close();
            is.close();

            playlist.setCustomThumbnailPath(outFile.getAbsolutePath());
            db.playlistDao().update(playlist);
            loadPlaylists();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
