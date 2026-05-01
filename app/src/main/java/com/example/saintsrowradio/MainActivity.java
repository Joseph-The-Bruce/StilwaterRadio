package com.example.saintsrowradio;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionToken;

import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private String activeStationId = "";
    
    // Default settings values
    private int commercialsPerSong = 3;
    private int songsPerRotation = 1;
    private int songsBeforeNews = 5;
    private boolean includeSingAlongs = false;
    private boolean skipSplash = false;
    private boolean disableMenuMusic = false;
    
    // Saints Radio station inclusions
    private boolean includeKrunch = true;
    private boolean includeKrhyme = true;
    private boolean includeMix = true;
    private boolean includeGenx = true;
    private boolean includeEzzzy = true;
    private boolean includeUndrgrnd = true;
    private boolean includeUltor = true;
    private boolean includeWorld = true;
    private boolean includeFour20 = true;
    private boolean includeFunk = true;
    private boolean includeK12 = true;
    private boolean includeKlassic = true;

    public static final String PREFS_NAME = "SaintsRadioPrefs";
    private static final String KEY_COMMERCIALS = "commercialsPerSong";
    private static final String KEY_SONGS_PER_ROTATION = "songsPerRotation";
    private static final String KEY_NEWS = "songsBeforeNews";
    private static final String KEY_SING_ALONGS = "includeSingAlongs";
    public static final String KEY_SKIP_SPLASH = "skipSplash";
    private static final String KEY_DISABLE_MENU_MUSIC = "disableMenuMusic";
    
    private static final String KEY_INCLUDE_KRUNCH = "includeKrunch";
    private static final String KEY_INCLUDE_KRHYME = "includeKrhyme";
    private static final String KEY_INCLUDE_MIX = "includeMix";
    private static final String KEY_INCLUDE_GENX = "includeGenx";
    private static final String KEY_INCLUDE_EZZZY = "includeEzzzy";
    private static final String KEY_INCLUDE_UNDRGRND = "includeUndrgrnd";
    private static final String KEY_INCLUDE_ULTOR = "includeUltor";
    private static final String KEY_INCLUDE_WORLD = "includeWorld";
    private static final String KEY_INCLUDE_FOUR20 = "includeFour20";
    private static final String KEY_INCLUDE_FUNK = "includeFunk";
    private static final String KEY_INCLUDE_K12 = "includeK12";
    private static final String KEY_INCLUDE_KLASSIC = "includeKlassic";


    public static final String Broadcast_START_SAINTS_RADIO = "com.example.saintsrowradio.StartSaintsRadio";
    public static final String Broadcast_START_KRUNCH_RADIO = "com.example.saintsrowradio.StartKrunchRadio";
    public static final String Broadcast_START_KRHYME_RADIO = "com.example.saintsrowradio.StartKrhymeRadio";
    public static final String Broadcast_START_MIX_RADIO = "com.example.saintsrowradio.StartMixRadio";
    public static final String Broadcast_START_GENX_RADIO = "com.example.saintsrowradio.StartGenxRadio";
    public static final String Broadcast_START_EZZZY_RADIO = "com.example.saintsrowradio.StartEzzzyRadio";
    public static final String Broadcast_START_UNDRGRND_RADIO = "com.example.saintsrowradio.StartUndrgrndRadio";
    public static final String Broadcast_START_ULTOR_RADIO = "com.example.saintsrowradio.StartUltorRadio";
    public static final String Broadcast_START_WORLD_RADIO = "com.example.saintsrowradio.StartWorldRadio";
    public static final String Broadcast_START_FOUR20_RADIO = "com.example.saintsrowradio.StartFour20Radio";
    public static final String Broadcast_START_FUNK_RADIO = "com.example.saintsrowradio.StartFunkRadio";
    public static final String Broadcast_START_K12_RADIO = "com.example.saintsrowradio.StartK12Radio";
    public static final String Broadcast_START_KLASSIC_RADIO = "com.example.saintsrowradio.StartKlassicRadio";

    private final int[] backgrounds = {
            R.drawable.saintsrow2,
            R.drawable.promo,
            R.drawable.saints_promo,
            R.drawable.saints_large_logo,
            R.drawable.white_shirt,
            R.drawable.white_suit,
            R.drawable.saints_tag1,
            R.drawable.saints_tag2,
            R.drawable.saints_tag3,
            R.drawable.saints_tag4,
            R.drawable.saints_tag5,
            R.drawable.saints_tag6,
            R.drawable.saints_tag7,
            R.drawable.saints_tag8
    };
    private int currentBackgroundResId = R.drawable.saintsrow2;
    private final Random random = new Random();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initializeMediaController();
                }
            });

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            initializeMediaController();
        }
    }

    private void initializeMediaController() {
        if (controllerFuture == null) {
            SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MediaPlayerService.class));
            controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
            controllerFuture.addListener(() -> {
                try {
                    mediaController = controllerFuture.get();
                    mediaController.addListener(new Player.Listener() {
                        @Override
                        public void onIsPlayingChanged(boolean isPlaying) {
                            updatePlayPauseButton(isPlaying);
                        }

                        @Override
                        public void onPlaybackStateChanged(int playbackState) {
                            updatePlayPauseButton(mediaController.isPlaying());
                        }

                        @Override
                        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                            syncActiveStation(mediaItem);
                        }
                    });
                    // Sync settings to service immediately after connection
                    updateServiceSettings();
                    syncActiveStation(mediaController.getCurrentMediaItem());
                    updatePlayPauseButton(mediaController.isPlaying());
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "MediaController connection failed", e);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private void updateStationButtonStates() {
        int[] buttonIds = {
                R.id.toggleButton, R.id.toggleButton1, R.id.toggleButton2, R.id.toggleButton3,
                R.id.toggleButton4, R.id.toggleButton5, R.id.toggleButton6, R.id.toggleButton7,
                R.id.toggleButton8, R.id.toggleButton9, R.id.toggleButton10, R.id.toggleButton11,
                R.id.toggleButton12
        };
        String[] stationIds = {
                "saints", "krunch", "krhyme", "mix", "genx", "ezzzy", "undrgrnd", "ultor", "world", "four20", "funk", "k12", "klassic"
        };

        boolean nothingActive = activeStationId == null || activeStationId.isEmpty();

        for (int i = 0; i < buttonIds.length; i++) {
            View v = findViewById(buttonIds[i]);
            if (v != null) {
                // If nothing is active, keep everything at full brightness.
                // If a station is active, highlight only the active one.
                if (nothingActive || activeStationId.equals(stationIds[i])) {
                    v.setAlpha(1.0f);
                } else {
                    v.setAlpha(0.5f); // Dim the inactive stations
                }
            }
        }
    }

    private void syncActiveStation(MediaItem item) {
        if (item != null) {
            // Priority 1: Use the explicit stationId extra if available
            if (item.mediaMetadata.extras != null && item.mediaMetadata.extras.containsKey("stationId")) {
                activeStationId = item.mediaMetadata.extras.getString("stationId");
                updateStationButtonStates();
                return;
            }

            // Priority 2: Handle special media IDs like the pause menu
            String id = item.mediaId;
            if (id.equals("shared_pausemenu") || id.startsWith("shared_")) {
                activeStationId = "";
            } else if (id.contains("_")) {
                // Priority 3: Infer station from ID prefix (e.g., "krunch_song" -> "krunch")
                activeStationId = id.substring(0, id.indexOf("_"));
            } else {
                activeStationId = id;
            }
        }
        updateStationButtonStates();
    }

    private void applyBackground() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            boolean isPortrait = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            int bgToApply = currentBackgroundResId;

            if (isPortrait) {
                if (currentBackgroundResId == R.drawable.saints_large_logo) {
                    bgToApply = R.drawable.saints_logo_pattern_portrait;
                } else if (currentBackgroundResId == R.drawable.saintsrow2) {
                    bgToApply = R.drawable.saintsrow2_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag1) {
                    bgToApply = R.drawable.saints_tag1_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag2) {
                    bgToApply = R.drawable.saints_tag2_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag3) {
                    bgToApply = R.drawable.saints_tag3_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag4) {
                    bgToApply = R.drawable.saints_tag4_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag5) {
                    bgToApply = R.drawable.saints_tag5_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag6) {
                    bgToApply = R.drawable.saints_tag6_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag7) {
                    bgToApply = R.drawable.saints_tag7_portrait;
                } else if (currentBackgroundResId == R.drawable.saints_tag8) {
                    bgToApply = R.drawable.saints_tag8_portrait;
                }
            }
            
            mainView.setBackgroundResource(bgToApply);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentBackground", currentBackgroundResId);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        ImageButton playPauseButton = findViewById(R.id.playPauseButton);
        if (playPauseButton != null) {
            if (isPlaying) {
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
            mediaController = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem singAlongItem = menu.findItem(R.id.action_toggle_singalong);
        if (singAlongItem != null) {
            singAlongItem.setChecked(includeSingAlongs);
        }
        MenuItem skipSplashItem = menu.findItem(R.id.action_toggle_skip_splash);
        if (skipSplashItem != null) {
            skipSplashItem.setChecked(skipSplash);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            showRotationSettingsDialog();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_singalong) {
            includeSingAlongs = !item.isChecked();
            item.setChecked(includeSingAlongs);
            saveSettings();
            updateServiceSettings();
            return true;
        } else if (item.getItemId() == R.id.action_toggle_skip_splash) {
            skipSplash = !item.isChecked();
            item.setChecked(skipSplash);
            saveSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        commercialsPerSong = prefs.getInt(KEY_COMMERCIALS, 3);
        songsPerRotation = prefs.getInt(KEY_SONGS_PER_ROTATION, 1);
        songsBeforeNews = prefs.getInt(KEY_NEWS, 5);
        includeSingAlongs = prefs.getBoolean(KEY_SING_ALONGS, false);
        skipSplash = prefs.getBoolean(KEY_SKIP_SPLASH, false);
        disableMenuMusic = prefs.getBoolean(KEY_DISABLE_MENU_MUSIC, false);
        
        includeKrunch = prefs.getBoolean(KEY_INCLUDE_KRUNCH, true);
        includeKrhyme = prefs.getBoolean(KEY_INCLUDE_KRHYME, true);
        includeMix = prefs.getBoolean(KEY_INCLUDE_MIX, true);
        includeGenx = prefs.getBoolean(KEY_INCLUDE_GENX, true);
        includeEzzzy = prefs.getBoolean(KEY_INCLUDE_EZZZY, true);
        includeUndrgrnd = prefs.getBoolean(KEY_INCLUDE_UNDRGRND, true);
        includeUltor = prefs.getBoolean(KEY_INCLUDE_ULTOR, true);
        includeWorld = prefs.getBoolean(KEY_INCLUDE_WORLD, true);
        includeFour20 = prefs.getBoolean(KEY_INCLUDE_FOUR20, true);
        includeFunk = prefs.getBoolean(KEY_INCLUDE_FUNK, true);
        includeK12 = prefs.getBoolean(KEY_INCLUDE_K12, true);
        includeKlassic = prefs.getBoolean(KEY_INCLUDE_KLASSIC, true);
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COMMERCIALS, commercialsPerSong);
        editor.putInt(KEY_SONGS_PER_ROTATION, songsPerRotation);
        editor.putInt(KEY_NEWS, songsBeforeNews);
        editor.putBoolean(KEY_SING_ALONGS, includeSingAlongs);
        editor.putBoolean(KEY_SKIP_SPLASH, skipSplash);
        editor.putBoolean(KEY_DISABLE_MENU_MUSIC, disableMenuMusic);
        
        editor.putBoolean(KEY_INCLUDE_KRUNCH, includeKrunch);
        editor.putBoolean(KEY_INCLUDE_KRHYME, includeKrhyme);
        editor.putBoolean(KEY_INCLUDE_MIX, includeMix);
        editor.putBoolean(KEY_INCLUDE_GENX, includeGenx);
        editor.putBoolean(KEY_INCLUDE_EZZZY, includeEzzzy);
        editor.putBoolean(KEY_INCLUDE_UNDRGRND, includeUndrgrnd);
        editor.putBoolean(KEY_INCLUDE_ULTOR, includeUltor);
        editor.putBoolean(KEY_INCLUDE_WORLD, includeWorld);
        editor.putBoolean(KEY_INCLUDE_FOUR20, includeFour20);
        editor.putBoolean(KEY_INCLUDE_FUNK, includeFunk);
        editor.putBoolean(KEY_INCLUDE_K12, includeK12);
        editor.putBoolean(KEY_INCLUDE_KLASSIC, includeKlassic);
        
        editor.apply();
    }

    private void updateServiceSettings() {
        if (mediaController != null) {
            Bundle args = new Bundle();
            args.putInt("commercialsPerSong", commercialsPerSong);
            args.putInt("songsPerRotation", songsPerRotation);
            args.putInt("songsBeforeNews", songsBeforeNews);
            args.putBoolean("includeSingAlongs", includeSingAlongs);
            args.putBoolean("disableMenuMusic", disableMenuMusic);
            
            args.putBoolean("includeKrunch", includeKrunch);
            args.putBoolean("includeKrhyme", includeKrhyme);
            args.putBoolean("includeMix", includeMix);
            args.putBoolean("includeGenx", includeGenx);
            args.putBoolean("includeEzzzy", includeEzzzy);
            args.putBoolean("includeUndrgrnd", includeUndrgrnd);
            args.putBoolean("includeUltor", includeUltor);
            args.putBoolean("includeWorld", includeWorld);
            args.putBoolean("includeFour20", includeFour20);
            args.putBoolean("includeFunk", includeFunk);
            args.putBoolean("includeK12", includeK12);
            args.putBoolean("includeKlassic", includeKlassic);
            
            mediaController.sendCustomCommand(new SessionCommand("ACTION_UPDATE_SETTINGS", Bundle.EMPTY), args);
        }
    }

    private void showRotationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_settings);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);
        scrollView.addView(layout);

        // Commercials Slider
        final TextView commLabel = new TextView(this);
        commLabel.setText(getString(R.string.settings_commercials_label, commercialsPerSong));
        commLabel.setPadding(0, 20, 0, 0);
        layout.addView(commLabel);

        final Slider commSlider = new Slider(this);
        commSlider.setValueFrom(0f);
        commSlider.setValueTo(5f);
        commSlider.setStepSize(1f);
        commSlider.setValue((float) commercialsPerSong);
        commSlider.addOnChangeListener((slider, value, fromUser) -> commLabel.setText(getString(R.string.settings_commercials_label, (int)value)));
        layout.addView(commSlider);

        // Songs per Rotation Slider
        final TextView songsPerRotationLabel = new TextView(this);
        songsPerRotationLabel.setText(getString(R.string.settings_songs_per_rotation_label, songsPerRotation));
        songsPerRotationLabel.setPadding(0, 40, 0, 0);
        layout.addView(songsPerRotationLabel);

        final Slider songsPerRotationSlider = new Slider(this);
        songsPerRotationSlider.setValueFrom(1f);
        songsPerRotationSlider.setValueTo(5f);
        songsPerRotationSlider.setStepSize(1f);
        songsPerRotationSlider.setValue((float) songsPerRotation);
        songsPerRotationSlider.addOnChangeListener((slider, value, fromUser) -> songsPerRotationLabel.setText(getString(R.string.settings_songs_per_rotation_label, (int)value)));
        layout.addView(songsPerRotationSlider);

        // News Slider
        final TextView newsLabel = new TextView(this);
        newsLabel.setText(getString(R.string.settings_news_label, songsBeforeNews));
        newsLabel.setPadding(0, 40, 0, 0);
        layout.addView(newsLabel);

        final Slider newsSlider = new Slider(this);
        newsSlider.setValueFrom(0f);
        newsSlider.setValueTo(10f);
        newsSlider.setStepSize(1f);
        newsSlider.setValue((float) songsBeforeNews);
        newsSlider.addOnChangeListener((slider, value, fromUser) -> newsLabel.setText(getString(R.string.settings_news_label, (int)value)));
        layout.addView(newsSlider);

        // Get primary color from theme to match sliders and menu
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;
        ColorStateList checkboxTint = ColorStateList.valueOf(colorPrimary);

        // Sing Along Label
        final TextView singAlongLabel = new TextView(this);
        singAlongLabel.setText(R.string.settings_singalong_label);
        singAlongLabel.setPadding(0, 40, 0, 0);
        layout.addView(singAlongLabel);

        // Sing Along Checkbox
        final CheckBox singAlongCheckbox = new CheckBox(this);
        singAlongCheckbox.setChecked(includeSingAlongs);
        singAlongCheckbox.setButtonTintList(checkboxTint);
        layout.addView(singAlongCheckbox);

        // Skip Splash Label
        final TextView skipSplashLabel = new TextView(this);
        skipSplashLabel.setText(R.string.action_skip_splash);
        skipSplashLabel.setPadding(0, 40, 0, 0);
        layout.addView(skipSplashLabel);

        // Skip Splash Checkbox
        final CheckBox skipSplashCheckbox = new CheckBox(this);
        skipSplashCheckbox.setChecked(skipSplash);
        skipSplashCheckbox.setButtonTintList(checkboxTint);
        layout.addView(skipSplashCheckbox);

        // Disable Menu Music Label
        final TextView disableMenuMusicLabel = new TextView(this);
        disableMenuMusicLabel.setText(R.string.settings_menu_music_label);
        disableMenuMusicLabel.setPadding(0, 40, 0, 0);
        layout.addView(disableMenuMusicLabel);

        // Disable Menu Music Checkbox
        final CheckBox disableMenuMusicCheckbox = new CheckBox(this);
        disableMenuMusicCheckbox.setChecked(disableMenuMusic);
        disableMenuMusicCheckbox.setButtonTintList(checkboxTint);
        layout.addView(disableMenuMusicCheckbox);
        
        // Saints Radio Station Inclusions
        final TextView saintsLabel = new TextView(this);
        saintsLabel.setText(R.string.saints_radio_stations);
        saintsLabel.setPadding(0, 40, 0, 0);
        layout.addView(saintsLabel);
        
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(4);
        gridLayout.setPadding(0, 10, 0, 0);
        
        final CheckBox krunchCheck = createStationCheckbox(getString(R.string.station_krunch), includeKrunch, checkboxTint);
        final CheckBox krhymeCheck = createStationCheckbox(getString(R.string.station_krhyme), includeKrhyme, checkboxTint);
        final CheckBox mixCheck = createStationCheckbox(getString(R.string.station_mix), includeMix, checkboxTint);
        final CheckBox genxCheck = createStationCheckbox(getString(R.string.station_genx), includeGenx, checkboxTint);
        final CheckBox ezzzyCheck = createStationCheckbox(getString(R.string.station_ezzzy), includeEzzzy, checkboxTint);
        final CheckBox undrgrndCheck = createStationCheckbox(getString(R.string.station_undrgrnd), includeUndrgrnd, checkboxTint);
        final CheckBox ultorCheck = createStationCheckbox(getString(R.string.station_ultor), includeUltor, checkboxTint);
        final CheckBox worldCheck = createStationCheckbox(getString(R.string.station_world), includeWorld, checkboxTint);
        final CheckBox four20Check = createStationCheckbox(getString(R.string.station_four20), includeFour20, checkboxTint);
        final CheckBox funkCheck = createStationCheckbox(getString(R.string.station_funk), includeFunk, checkboxTint);
        final CheckBox k12Check = createStationCheckbox(getString(R.string.station_k12), includeK12, checkboxTint);
        final CheckBox klassicCheck = createStationCheckbox(getString(R.string.station_klassic), includeKlassic, checkboxTint);
        
        gridLayout.addView(krunchCheck);
        gridLayout.addView(krhymeCheck);
        gridLayout.addView(mixCheck);
        gridLayout.addView(genxCheck);
        gridLayout.addView(ezzzyCheck);
        gridLayout.addView(undrgrndCheck);
        gridLayout.addView(ultorCheck);
        gridLayout.addView(worldCheck);
        gridLayout.addView(four20Check);
        gridLayout.addView(funkCheck);
        gridLayout.addView(k12Check);
        gridLayout.addView(klassicCheck);
        
        layout.addView(gridLayout);

        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Validation: at least 2 stations
            int count = 0;
            if (krunchCheck.isChecked()) count++;
            if (krhymeCheck.isChecked()) count++;
            if (mixCheck.isChecked()) count++;
            if (genxCheck.isChecked()) count++;
            if (ezzzyCheck.isChecked()) count++;
            if (undrgrndCheck.isChecked()) count++;
            if (ultorCheck.isChecked()) count++;
            if (worldCheck.isChecked()) count++;
            if (four20Check.isChecked()) count++;
            if (funkCheck.isChecked()) count++;
            if (k12Check.isChecked()) count++;
            if (klassicCheck.isChecked()) count++;
            
            if (count < 2) {
                Toast.makeText(this, "Please select at least 2 stations for Saints Radio", Toast.LENGTH_LONG).show();
                showRotationSettingsDialog(); 
                return;
            }
            
            commercialsPerSong = (int) commSlider.getValue();
            songsPerRotation = (int) songsPerRotationSlider.getValue();
            songsBeforeNews = (int) newsSlider.getValue();
            includeSingAlongs = singAlongCheckbox.isChecked();
            skipSplash = skipSplashCheckbox.isChecked();
            disableMenuMusic = disableMenuMusicCheckbox.isChecked();
            
            includeKrunch = krunchCheck.isChecked();
            includeKrhyme = krhymeCheck.isChecked();
            includeMix = mixCheck.isChecked();
            includeGenx = genxCheck.isChecked();
            includeEzzzy = ezzzyCheck.isChecked();
            includeUndrgrnd = undrgrndCheck.isChecked();
            includeUltor = ultorCheck.isChecked();
            includeWorld = worldCheck.isChecked();
            includeFour20 = four20Check.isChecked();
            includeFunk = funkCheck.isChecked();
            includeK12 = k12Check.isChecked();
            includeKlassic = klassicCheck.isChecked();
            
            saveSettings();
            updateServiceSettings();
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    
    private CheckBox createStationCheckbox(String text, boolean checked, ColorStateList tint) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setChecked(checked);
        cb.setButtonTintList(tint);
        return cb;
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void handleStationClick(String stationId, String broadcastAction) {
        vibrate();
        if (mediaController != null) {
            if (activeStationId.equals(stationId) && mediaController.isPlaying()) {
                mediaController.pause();
            } else if (activeStationId.equals(stationId) && !mediaController.isPlaying()) {
                mediaController.play();
            } else {
                activeStationId = stationId;
                updateStationButtonStates();
                sendBroadcast(new Intent(broadcastAction));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();
        if (savedInstanceState != null) {
            currentBackgroundResId = savedInstanceState.getInt("currentBackground", R.drawable.saintsrow2);
        } else {
            // New startup - pick random background
            currentBackgroundResId = backgrounds[random.nextInt(backgrounds.length)];
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        applyBackground();
        updateStationButtonStates();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
        }

        // Settings Button
        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            vibrate();
            showRotationSettingsDialog();
        });

        // Back Button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            vibrate();
            if (mediaController != null) {
                mediaController.sendCustomCommand(new SessionCommand("ACTION_SKIP_BACK", Bundle.EMPTY), Bundle.EMPTY);
            }
        });

        // Play/Pause Button
        ImageButton playPauseButton = findViewById(R.id.playPauseButton);
        playPauseButton.setOnClickListener(v -> {
            vibrate();
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        // Skip Button
        ImageButton skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(v -> {
            vibrate();
            if (mediaController != null) {
                mediaController.sendCustomCommand(new SessionCommand("ACTION_SKIP_NEXT", Bundle.EMPTY), Bundle.EMPTY);
            }
        });

        // Station buttons with toggle logic
        findViewById(R.id.toggleButton).setOnClickListener(v -> handleStationClick("saints", Broadcast_START_SAINTS_RADIO));
        findViewById(R.id.toggleButton1).setOnClickListener(v -> handleStationClick("krunch", Broadcast_START_KRUNCH_RADIO));
        findViewById(R.id.toggleButton2).setOnClickListener(v -> handleStationClick("krhyme", Broadcast_START_KRHYME_RADIO));
        findViewById(R.id.toggleButton3).setOnClickListener(v -> handleStationClick("mix", Broadcast_START_MIX_RADIO));
        findViewById(R.id.toggleButton4).setOnClickListener(v -> handleStationClick("genx", Broadcast_START_GENX_RADIO));
        findViewById(R.id.toggleButton5).setOnClickListener(v -> handleStationClick("ezzzy", Broadcast_START_EZZZY_RADIO));
        findViewById(R.id.toggleButton6).setOnClickListener(v -> handleStationClick("undrgrnd", Broadcast_START_UNDRGRND_RADIO));
        findViewById(R.id.toggleButton7).setOnClickListener(v -> handleStationClick("ultor", Broadcast_START_ULTOR_RADIO));
        findViewById(R.id.toggleButton8).setOnClickListener(v -> handleStationClick("world", Broadcast_START_WORLD_RADIO));
        findViewById(R.id.toggleButton9).setOnClickListener(v -> handleStationClick("four20", Broadcast_START_FOUR20_RADIO));
        findViewById(R.id.toggleButton10).setOnClickListener(v -> handleStationClick("funk", Broadcast_START_FUNK_RADIO));
        findViewById(R.id.toggleButton11).setOnClickListener(v -> handleStationClick("k12", Broadcast_START_K12_RADIO));
        findViewById(R.id.toggleButton12).setOnClickListener(v -> handleStationClick("klassic", Broadcast_START_KLASSIC_RADIO));
    }
}
