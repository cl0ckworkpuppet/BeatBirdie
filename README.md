# BeatBirdie: The Purist's Android Music Experience

BeatBirdie is a high-performance, local-first music player for Android that bridges the gap between simplicity and advanced playback logic. Built for users who value their local library, BeatBirdie focuses on audio integrity, algorithmic shuffle variety, and a seamless "Edge-to-Edge" UI.

---

## Key Product Pillars

### 1. Advanced Shuffle Intelligence (The "Fair Play" Engine)
Unlike standard players that use basic randomization, BeatBirdie features a custom Shuffle Algorithm Suite. Users can choose how they want to experience their music:
*   **Fisher-Yates**: Pure, mathematically unbiased shuffling.
*   **True Random**: For the unpredictable.
*   **Fair Play**: A smart algorithm that prevents artist-clumping and ensures a diverse listening session.

### 2. High-Performance Playback Architecture
At the heart of BeatBirdie is a centralized `PlaybackHandler` built on ExoPlayer. This ensures:
*   **Persistent Audio**: Zero-gap playback when navigating between the library and the full-player view.
*   **Intelligent Queueing**: A "Play Next" system that lets you inject songs into your current session without ruining your carefully curated queue.

### 3. Modern "Edge-to-Edge" Interface
Designed for modern Android displays, the app utilizes every pixel.
*   **Persistent Now Playing Bar**: Control your music from anywhere with zero friction.
*   **Metadata Fallback**: Even if your files aren't tagged, BeatBirdie’s "Smart Parsing" ensures your library looks clean by using filesystem heuristics.

---

## Setup & Deployment

### Developer Prerequisites
*   **Android SDK**: API 24 (Nougat) or higher.
*   **Build Tool**: Android Studio (Modern releases) with Gradle.

### Installation
1.  Clone the repository.
2.  Open in Android Studio and perform a Gradle Sync.
3.  Deploy to an emulator or physical device.

### Populating the Library
To test the scanning engine on an emulator:
1.  Drag any `.mp3` or `.m4a` file onto the emulator window.
2.  The file will land in `/sdcard/Download`.
3.  Use the **Refresh Library** menu item in BeatBirdie to trigger the `MediaStore` indexer.

---

## The User Journey: First-Time Setup & Use

### 1. Initial Launch & Permissions
When you open BeatBirdie for the first time, the app initiates its privacy-first onboarding:
*   **Rationale Dialog**: You will be greeted with an explanation of why the app requires file access. BeatBirdie needs this to scan your storage for audio files; without it, your music library cannot be displayed.
*   **Granting Access**: Upon clicking "Allow Access," the standard Android system permission prompt will appear. Ensure you select "Allow" or "Allow access to music and audio" (on Android 13+).
*   **Immediate Scanning**: Once permission is granted, BeatBirdie automatically triggers its internal scanner to find and index your music in seconds.

### 2. Browsing Your Music Library
With permissions granted, you are taken to the main library screen:
*   **The Song List**: All discovered audio tracks are displayed in a clean list, sorted alphabetically by title.
*   **Smart Metadata**: The app displays the song title, artist, and album. If a file is missing tags, BeatBirdie intelligently uses the filename as the title and labels the artist as "Unknown Artist" so no track is left behind.

### 3. Playing Your First Song
*   **Instant Playback**: Tap any song in the list to start listening immediately.
*   **Now Playing Bar**: As soon as a song starts, a "Now Playing" bar appears at the bottom. This bar is persistent—it stays visible even as you scroll through hundreds of songs, giving you constant access to Play/Pause controls and song info.

### 4. Exploring the Full Player (Focus View)
Tap the "Now Playing" bar at the bottom to expand it into the full-screen Focus View:
*   **Full Metadata**: See the artist, album, and title in a dedicated space.
*   **Scrubber Control**: Drag the seek bar (scrubber) to jump to any point in the song.
*   **Media Controls**: Use the large Play/Pause, Skip, and Rewind buttons. 
    *   *Tip*: Tapping Rewind after 5 seconds of playback will restart the current track. Tapping it earlier will skip back to the previous track in the queue.

### 5. Mastering Your Session
*   **Queue Management**: Use the queueing features to tailor your session. You can Play Next (add to front) to prioritize a song without stopping what's currently playing, or remove tracks from the current queue to skip them entirely.
*   **Refresh Library**: If you download or copy new music to your device while the app is open, simply tap the three dots in the top-right corner and select "Refresh Library" to sync your list without needing to restart the app.
*   **Custom Shuffle Logic**: Visit the Settings menu to change how your music is shuffled, choosing between Fisher-Yates, True Random, or the clumping-resistant Fair Play algorithm.

---

## Technical Stack

*   **Language**: Java (Modern Android standards)
*   **Audio Engine**: ExoPlayer / MediaPlayer (Ensures support for various codecs).
*   **Database**: Android MediaStore API (Hardware-accelerated file discovery).
*   **UI Framework**: Material Design 3 / ConstraintLayout.

---

## Privacy Commitment
BeatBirdie is 100% offline. It does not track your listening habits, require an account, or transmit your library data to any external servers. Your music, your device, your privacy.
