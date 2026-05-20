package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Custom Glide model and loader for extracting embedded album art from audio files.
 * This ensures high accuracy (unique art for "Singles") while benefiting from Glide's caching.
 */
public class AudioCoverModel {
    private final Uri uri;

    public AudioCoverModel(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioCoverModel that = (AudioCoverModel) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    public static class Loader implements ModelLoader<AudioCoverModel, InputStream> {
        private final Context context;

        public Loader(Context context) {
            this.context = context;
        }

        @Nullable
        @Override
        public LoadData<InputStream> buildLoadData(@NonNull AudioCoverModel model, int width, int height, @NonNull Options options) {
            return new LoadData<>(new ObjectKey(model.getUri()), new Fetcher(context, model.getUri()));
        }

        @Override
        public boolean handles(@NonNull AudioCoverModel model) {
            return true;
        }
    }

    public static class Fetcher implements DataFetcher<InputStream> {
        private final Context context;
        private final Uri uri;

        public Fetcher(Context context, Uri uri) {
            this.context = context;
            this.uri = uri;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, uri);
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    callback.onDataReady(new ByteArrayInputStream(art));
                } else {
                    callback.onDataReady(null);
                }
            } catch (Exception e) {
                callback.onDataReady(null);
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }
        }

        @Override
        public void cleanup() {
            // No resources to clean up manually
        }

        @Override
        public void cancel() {
            // Cannot cancel MMR easily
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }

    public static class Factory implements ModelLoaderFactory<AudioCoverModel, InputStream> {
        private final Context context;

        public Factory(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ModelLoader<AudioCoverModel, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new Loader(context);
        }

        @Override
        public void teardown() {
        }
    }
}
