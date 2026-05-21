package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NowPlayingActivity extends AppCompatActivity implements PlaybackHandler.PlaybackListener {

    private ImageButton playPause;
    private TextView songTitle;
    private TextView artistAlbum;
    private ImageView albumCover;
    private SeekBar scrubberBar;
    private CheckBox repeatCheckbox;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBar;
    private SeekBar volumeSeekBar;
    private AudioManager audioManager;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private QueueAdapter queueAdapter;
    private RecyclerView queueRecyclerView;
    private Spinner shuffleSpinner;
    private String[] currentAlgorithms;

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.now_playing_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackNavigation();
            return true;
        } else if (item.getItemId() == R.id.action_add_to_playlist) {
            showAddToPlaylistDialog();
            return true;
        } else if (item.getItemId() == R.id.action_blacklist) {
            confirmBlacklist();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleBackNavigation() {
        int playlistId = PlaybackHandler.getPlayingPlaylistId();
        if (playlistId != -1) {
            AppDatabase db = AppDatabase.getInstance(this);
            Playlist playlist = db.playlistDao().getPlaylistById(playlistId);
            if (playlist != null) {
                Intent intent = new Intent(this, PlaylistDetailActivity.class);
                intent.putExtra("playlist_id", playlist.getId());
                intent.putExtra("playlist_name", playlist.getName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return;
            }
        }
        finish();
    }

    private void showAddToPlaylistDialog() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong == null) return;

        AppDatabase db = AppDatabase.getInstance(this);
        List<Playlist> allPlaylists = db.playlistDao().getAllPlaylists();
        List<Integer> existingPlaylistIds = db.playlistDao().getPlaylistIdsForSong(currentSong.getPath());

        List<Playlist> eligiblePlaylists = new ArrayList<>();
        for (Playlist p : allPlaylists) {
            if (!existingPlaylistIds.contains(p.getId())) {
                eligiblePlaylists.add(p);
            }
        }

        if (eligiblePlaylists.isEmpty()) {
            Toast.makeText(this, "No eligible playlists found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playlistNames = new String[eligiblePlaylists.size()];
        for (int i = 0; i < eligiblePlaylists.size(); i++) {
            playlistNames[i] = eligiblePlaylists.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Add to Playlist")
                .setItems(playlistNames, (dialog, which) -> {
                    Playlist selected = eligiblePlaylists.get(which);
                    
                    // Double check with a failsafe check
                    if (db.playlistDao().isSongInPlaylist(selected.getId(), currentSong.getPath())) {
                        Toast.makeText(this, "Error: Song already in this playlist.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.playlistDao().addSongToPlaylist(new PlaylistSong(selected.getId(), currentSong.getPath()));
                    db.playlistDao().updateSongCount(selected.getId());
                    
                    // If the selected playlist is currently playing, update its queue
                    if (PlaybackHandler.getPlayingPlaylistId() == selected.getId()) {
                        List<String> paths = db.playlistDao().getSongPathsForPlaylist(selected.getId());
                        List<Song> updatedSongs = new ArrayList<>();
                        for (String path : paths) {
                            Song s = Song.getByPath(this, path);
                            if (s != null) updatedSongs.add(s);
                        }
                        PlaybackHandler.updatePlaylist(updatedSongs, selected.getId());
                    }
                    
                    Toast.makeText(this, "Added to " + selected.getName(), Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmBlacklist() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Blacklist Song")
                .setMessage("Are you sure you want to blacklist \"" + currentSong.getTitle() + "\"? It will be removed from your library and skipped.")
                .setPositiveButton("Blacklist", (dialog, which) -> {
                    BlacklistManager.add(this, currentSong.getPath());
                    Toast.makeText(this, "Song blacklisted.", Toast.LENGTH_SHORT).show();
                    // Update current playlist in handler. updatePlaylist will handle the skip if it was playing.
                    List<Song> queue = PlaybackHandler.getFullQueue();
                    queue.remove(currentSong);
                    PlaybackHandler.updatePlaylist(queue, PlaybackHandler.getPlayingPlaylistId());
                    updateUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onSongChanged() {
        runOnUiThread(() -> {
            updateUI();
            if (queueAdapter != null) {
                refreshQueueData();
            }
        });
    }

    @Override
    public void onQueueModified() {
        runOnUiThread(() -> {
            if (queueAdapter != null) {
                refreshQueueData();
            }
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            updateShuffleSpinnerSelection();
        });
    }

    private void updateShuffleSpinnerSelection() {
        if (shuffleSpinner == null || currentAlgorithms == null) return;
        String currentAlg = PlaybackHandler.currentAlg();
        for (int i = 0; i < currentAlgorithms.length; i++) {
            if (currentAlgorithms[i].equals(currentAlg)) {
                if (shuffleSpinner.getSelectedItemPosition() != i) {
                    shuffleSpinner.setSelection(i);
                }
                break;
            }
        }
    }

    private void refreshQueueData() {
        List<Song> fullQueue = PlaybackHandler.getFullQueue();
        int currentIndex = PlaybackHandler.getQueueOrderIndex();
        
        Song currentSong = null;
        List<Song> upNext = new ArrayList<>();
        
        if (currentIndex != -1 && currentIndex < fullQueue.size()) {
            currentSong = fullQueue.get(currentIndex);
            for (int i = currentIndex + 1; i < fullQueue.size(); i++) {
                upNext.add(fullQueue.get(i));
            }
        }
        
        // Update the static Now Playing section in the dialog if visible
        if (currentSongSectionView != null) {
            updateCurrentSongSection(currentSongSectionView, currentSong);
        }
        
        if (queueAdapter != null) {
            queueAdapter.updateData(upNext);
        }
    }

    private View currentSongSectionView;

    private void updateUI() {
        Song currentSong = PlaybackHandler.getCurrentSong();
        if (currentSong != null) {
            // Only update text if it's different to prevent Marquee reset
            String title = currentSong.getTitle();
            if (!songTitle.getText().toString().equals(title)) {
                songTitle.setText(title);
                songTitle.setSelected(true); // Required for marquee to start
            }

            String details = currentSong.getArtist() + " - " + currentSong.getAlbum();
            if (!artistAlbum.getText().toString().equals(details)) {
                artistAlbum.setText(details);
                artistAlbum.setSelected(true); // Required for marquee to start
            }

            // Use a tag on the ImageView to track which song's art is currently loaded.
            // This prevents Glide from re-triggering (and potentially flickering) when 
            // the UI refreshes due to playback state changes or seek events.
            String songUriString = currentSong.getUri().toString();
            if (!songUriString.equals(albumCover.getTag())) {
                albumCover.setTag(songUriString);
                Glide.with(this)
                    .load(new AudioCoverModel(currentSong.getUri()))
                    .placeholder(R.drawable.ic_music_note)
                    .error(Glide.with(this)
                        .load(currentSong.getAlbumArtUri())
                        .error(R.drawable.ic_music_note))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(albumCover);
            }
            
            // Sync SeekBar with song duration and current position
            scrubberBar.setMax(PlaybackHandler.getDuration());
            scrubberBar.setProgress(PlaybackHandler.getCurrentPosition());

            int current = PlaybackHandler.getCurrentPosition();
            int total = PlaybackHandler.getDuration();

            currentTimeText.setText(formatTime(current));
            totalTimeText.setText(formatTime(total));
        }
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (PlaybackHandler.isPlaying()) {
            playPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void applyKeepScreenOn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean keepOn = prefs.getBoolean("lock_screen_on", false);
        View root = findViewById(R.id.main);
        if (root != null) {
            root.setKeepScreenOn(keepOn);
        }

        if (keepOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyKeepScreenOn();
        PlaybackHandler.addListener(this);
        updateUI();
        setupShuffleSpinner();
        // Start the periodic update
        handler.post(updateSeekBar);
        if (repeatCheckbox != null) {
            repeatCheckbox.setChecked(PlaybackHandler.isLooping());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlaybackHandler.removeListener(this);
        // Stop the periodic update to save resources
        handler.removeCallbacks(updateSeekBar);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_now_playing);

        songTitle = findViewById(R.id.songTitle);
        artistAlbum = findViewById(R.id.artistAlbum);
        albumCover = findViewById(R.id.albumCover);
        playPause = findViewById(R.id.playpause);
        scrubberBar = findViewById(R.id.scrubberBar);
        repeatCheckbox = findViewById(R.id.repeatCheckbox);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        ImageButton skipButton = findViewById(R.id.skip);
        ImageButton rewindButton = findViewById(R.id.rewind);
        shuffleSpinner = findViewById(R.id.shuffleSpinner);
        Button btnViewQueue = findViewById(R.id.btnViewQueue);
        SeekBar volumeSeekBar = findViewById(R.id.volumeSeekBar);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekBar.setMax(maxVolume);
        volumeSeekBar.setProgress(currentVolume);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        // --- View Queue Setup ---
        btnViewQueue.setOnClickListener(v -> showQueueDialog());

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

        // Add listener for user scrubbing
        scrubberBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    PlaybackHandler.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Initialize the Runnable for updating the SeekBar
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (PlaybackHandler.isPlaying()) {
                    int current = PlaybackHandler.getCurrentPosition();
                    int total = PlaybackHandler.getDuration();

                    scrubberBar.setProgress(PlaybackHandler.getCurrentPosition());

                    currentTimeText.setText(formatTime(current));
                    totalTimeText.setText(formatTime(total));
                }
                handler.postDelayed(this, 1000); // Update every 1 second
            }
        };
        
        updateUI();
    }

    private void setupShuffleSpinner() {
        if (shuffleSpinner == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean experimentalEnabled = prefs.getBoolean("experimental_shuffle_enabled", false);

        List<String> algorithmList = new ArrayList<>(Arrays.asList("*Play in Order", "Fisher-Yates", "True Random, No Repeats", "Fair Play", "Sattolo's Algorithm", "Casino Shuffle", "Prime-Step", "*Bit-Reversal"));
        if (experimentalEnabled) {
            algorithmList.add("EXPERIMENTAL SHUFFLE");
        }
        currentAlgorithms = algorithmList.toArray(new String[0]);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, currentAlgorithms);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shuffleSpinner.setAdapter(spinnerAdapter);

        // Sync spinner with current PlaybackHandler state
        for (int i = 0; i < currentAlgorithms.length; i++) {
            if (currentAlgorithms[i].equals(PlaybackHandler.currentAlg())) {
                shuffleSpinner.setSelection(i);
                break;
            }
        }

        shuffleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedAlg = currentAlgorithms[position];
                if (!selectedAlg.equals(PlaybackHandler.currentAlg())) {
                    PlaybackHandler.setAlg(selectedAlg);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showQueueDialog() {
        List<Song> fullQueue = PlaybackHandler.getFullQueue();
        int currentIndex = PlaybackHandler.getQueueOrderIndex();
        
        if (fullQueue.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Queue")
                    .setMessage("The queue is empty.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_queue, null);
        currentSongSectionView = dialogView.findViewById(R.id.currentSongSection);
        queueRecyclerView = dialogView.findViewById(R.id.queueRecyclerView);
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        Song currentSong = null;
        List<Song> upNext = new ArrayList<>();
        
        if (currentIndex != -1 && currentIndex < fullQueue.size()) {
            currentSong = fullQueue.get(currentIndex);
            for (int i = currentIndex + 1; i < fullQueue.size(); i++) {
                upNext.add(fullQueue.get(i));
            }
        }
        
        updateCurrentSongSection(currentSongSectionView, currentSong);

        queueAdapter = new QueueAdapter(upNext, new QueueAdapter.OnQueueActionListener() {
            @Override
            public void onMoveToFront(int position) {
                // Adjust position because upNext is offset by currentIndex + 1
                int globalPos = PlaybackHandler.getQueueOrderIndex() + 1 + position;
                PlaybackHandler.moveSongToFront(globalPos);
            }

            @Override
            public void onRemove(int position) {
                int globalPos = PlaybackHandler.getQueueOrderIndex() + 1 + position;
                PlaybackHandler.removeSongFromQueue(globalPos);
            }
        });
        queueRecyclerView.setAdapter(queueAdapter);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", (d, which) -> {
                    queueAdapter = null;
                    queueRecyclerView = null;
                    currentSongSectionView = null;
                })
                .setOnDismissListener(d -> {
                    queueAdapter = null;
                    queueRecyclerView = null;
                    currentSongSectionView = null;
                })
                .create();
        
        dialog.show();

        // Make the dialog larger (85% of screen height and 90% of screen width)
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
            dialog.getWindow().setLayout(width, height);
        }
    }

    private void updateCurrentSongSection(View section, Song song) {
        if (section == null || song == null) return;
        
        TextView title = section.findViewById(R.id.songTitle);
        TextView details = section.findViewById(R.id.songDetails);
        ImageView cover = section.findViewById(R.id.queueAlbumCover);
        ImageButton move = section.findViewById(R.id.btnMoveFront);
        ImageButton remove = section.findViewById(R.id.btnRemove);
        
        title.setText(song.getTitle());
        details.setText(song.getArtist() + " • " + song.getAlbum());
        move.setVisibility(View.GONE);
        remove.setVisibility(View.GONE);
        
        // Load cover using robust AudioCoverModel
        Glide.with(this)
            .load(new AudioCoverModel(song.getUri()))
            .placeholder(R.drawable.ic_music_note)
            .error(Glide.with(this)
                .load(song.getAlbumArtUri())
                .error(R.drawable.ic_music_note))
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(cover);
    }

    private  String formatTime (int ms) {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
