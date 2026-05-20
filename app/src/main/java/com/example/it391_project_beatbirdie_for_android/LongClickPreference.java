package com.example.it391_project_beatbirdie_for_android;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class LongClickPreference extends Preference {

    public interface OnPreferenceLongClickListener {
        boolean onPreferenceLongClick(Preference preference);
    }

    private OnPreferenceLongClickListener onPreferenceLongClickListener;

    public LongClickPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LongClickPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LongClickPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LongClickPreference(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setOnLongClickListener(v -> {
            if (onPreferenceLongClickListener != null) {
                return onPreferenceLongClickListener.onPreferenceLongClick(this);
            }
            return false;
        });
    }

    public void setOnPreferenceLongClickListener(OnPreferenceLongClickListener listener) {
        this.onPreferenceLongClickListener = listener;
    }
}
