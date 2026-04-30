# BeatBirdie: Offline Music Shuffler for Android

BeatBirdie is a high-performance local music player for Android that bridges the gap between simplicity and advanced playback logic. Built for anyone who values having personal control over their music library, BeatBirdie puts the user experience above everything else.

---

## Highlighted Features

### 1. Advanced Shuffling Algorithms
Unlike standard players that use basic randomization or forces one type of shuffling strategy that may not work for all users, BeatBirdie features a custom Shuffle Algorithm selection tool. Users can choose how they want to experience their music:
*   **Fisher-Yates**: Walks through your music list and swaps each song with another song, chosen randomly.
*   **True Random**: Gives each song a random spot in line, and then plays them in that order.
*   **Fair Play**: Ensures a balanced variety by preventing the same artist or album from playing too frequently in a row.

### 2. High-Performance Playback Architecture
BeatBirdie uses a centralized `PlaybackHandler` built on ExoPlayer. This ensures:
*   **Persistent Audio**: Zero-gap playback when navigating between the library and the full-player view, or when putting the device to sleep.
*   **Intelligent Queueing**: Utilizes a dynamic, interactive queue that users can change, reshuffle, or delete from at will.

### 3. Modern "Edge-to-Edge" Interface
BeatBirdie is structured using modern design principles and utilizes every bit of onscreen space.
*   **Persistent Now Playing Bar**: Control your music from anywhere with zero friction, or tap it to access a more robust playback interface.
*   **Metadata Fallback**: Song metadata is accounted for, and supplemented in the event of a lack of certain fields.

---

## Simple Installation
For those without Android Studio on their computer, you can get this app on your Android device:
* Go to the [Releases](https://github.com/cl0ckworkpuppet/IT_391_Project/releases) page and download the latest version of the app.
* Transfer the APK to your Android device. [A tutorial on how to do this can be found here.](https://support.google.com/android/answer/9064445?hl=en)
* On your device, enable "Install unknown apps" (Settings → Security/Privacy → allow your file manager or browser).
* Open the APK file and install it.
* Launch the app from your app drawer.

---

## User First-Time Setup & Use

### 1. Initial Launch & Permissions
When you open BeatBirdie for the first time, the app initiates its onboarding:
*   **Rationale Dialog**: You will be greeted with an explanation of why the app requires file access. BeatBirdie needs this to scan your storage for audio files; without it, your music library cannot be displayed. *BeatBirdie will never access data beyond what is absolutely necessary, and will never distribute your data.*
*   **Granting Access**: Upon clicking "Allow Access," the standard Android system permission prompt will appear. Ensure you select "Allow" or "Allow access to music and audio" (on Android 13+).
*   **Immediate Scanning**: Once permission is granted, BeatBirdie automatically triggers its internal scanner to find and index your music in seconds.

### 2. Browsing Your Music Library
With permissions granted, you are taken to the main library screen:
*   **The Song List**: All discovered audio tracks are displayed in a clean list, sorted alphabetically by title.
*   **Smart Metadata**: The app displays the song title, artist, album name, and album art. If a file is missing tags, BeatBirdie automatically uses the filename as the title and labels the artist as "Unknown Artist" so no track is left behind. If a file doesn't have album art, a placeholder image will be used.

### 3. Playing Your First Song
*   **Instant Playback**: Tap any song in the list to start listening immediately.
*   **Now Playing Bar**: As soon as a song starts, a "Now Playing" bar appears at the bottom. This bar stays visible even as you scroll through your library, giving you constant access to Play/Pause controls and song info.

### 4. Exploring the Full Player (Focus View)
Tap the "Now Playing" bar at the bottom to expand it into the full-screen Focus View:
*   **Full Metadata**: See the artist, album, and title in a dedicated space.
*   **Scrubber Control**: Drag the seek bar (scrubber) to jump to any point in the song. You can also use the Volume slider to adjust your device volume.
*   **Media Controls**: Use the large Play/Pause, Skip, and Rewind buttons. You can also check Repeat to play one song on loop.
    *   *Tip*: Tapping Rewind after 5 seconds of playback will restart the current track. Tapping it earlier will skip back to the previous track in the queue.

### 5. Mastering Your Session
*   **Queue Management**: Use the queueing features to tailor your session. You can Play Next (add to front) to prioritize a song without stopping what's currently playing, or remove tracks from the current queue to skip them entirely.
*   **Refresh Library**: If you download or copy new music to your device while the app is open, simply tap the circular arrow icon in the top-right corner to sync your list without needing to restart the app.
*   **Custom Shuffle Logic**: Visit the Settings menu to change how your music is shuffled, choosing between Fisher-Yates, True Random, or the clumping-resistant Fair Play algorithm.
    *   *Tip*: You can also select your shuffle algorithm in the Focus View using the dropdown on the bottom-left.
*   **Sorting Your Music List**: Sorting by title isn't the only way to sort your library: by clicking the three bars on the top-right corner of your screen, you can choose to sort by title, artist name, or album name.

---

## Developer Setup & Deployment

### Prerequisites
*   **Android SDK**: API 24 (Nougat) or higher.
*   **Build Tool**: Android Studio (modern releases) with Gradle.

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

## Technical Stack

*   **Language**: Java
*   **Audio Engine**: ExoPlayer
*   **Database**: Android MediaStore API
*   **UI Framework**: Material Design 3 / ConstraintLayout

---

## Privacy Commitment
Pertaining to our user-first design philosophy, BeatBirdie is committed to user privacy. Our app is 100% offline. It does not track your listening habits, require an account, or transmit your library data to any external servers. BeatBirdie will never collect, save, or share your data.
