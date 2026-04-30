package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlaybackHandlerTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PlaybackHandler.init(context);
    }

    @Test
    public void testSongSelectionLogic() throws IOException {
        // Create a dummy file
        File testFile = new File(context.getCacheDir(), "test_song.mp3");
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write(new byte[1024]);
        }

        Uri testUri = Uri.fromFile(testFile);
        List<Song> songs = new ArrayList<>();
        // Fixed: The Song constructor requires 6 arguments: title, artist, album, uri, albumId (long), and duration (int).
        songs.add(new Song("Test Song", "Test Artist", "Test Album", testUri, 0L, 0));

        // Test setting the song
        // We call playSong, which internally sets the current song.
        // Even if MediaPlayer fails to play the dummy data, the current song should be set.
        PlaybackHandler.playSong(context, songs, 0);
        
        // Verify current song state
        Song current = PlaybackHandler.getCurrentSong();
        assertNotNull("Current song should be set in PlaybackHandler", current);
        assertEquals("Test Song", current.getTitle());
        assertEquals("Test Artist", current.getArtist());

        // Clean up
        if (testFile.exists()) {
            testFile.delete();
        }
    }
}
