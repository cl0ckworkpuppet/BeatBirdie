package com.example.it391_project_beatbirdie_for_android;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PlaylistActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlists);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.playlistRoot), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FloatingActionButton btnAddPlaylist = findViewById(R.id.btnAddPlaylist);

        PlaylistFragment playlistFragment = new PlaylistFragment();
        
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return playlistFragment;
                    case 1: return LibraryCategoryFragment.newInstance(LibraryCategoryFragment.TYPE_ALBUMS);
                    case 2: return LibraryCategoryFragment.newInstance(LibraryCategoryFragment.TYPE_ARTISTS);
                    case 3: return LibraryCategoryFragment.newInstance(LibraryCategoryFragment.TYPE_GENRES);
                    default: return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return 4;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Playlists"); break;
                case 1: tab.setText("Albums"); break;
                case 2: tab.setText("Artists"); break;
                case 3: tab.setText("Genres"); break;
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    btnAddPlaylist.show();
                } else {
                    btnAddPlaylist.hide();
                }
            }
        });

        btnAddPlaylist.setOnClickListener(v -> playlistFragment.showAddPlaylistDialog());
    }

    private void applyTheme() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String themeValue = prefs.getString("dark_mode", "default");
        switch (themeValue) {
            case "light":
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "default":
            default:
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
