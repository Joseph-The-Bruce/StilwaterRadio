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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionToken;

import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private String activeStationId = "";
    
    // Default settings values
    private int commercialsPerSong = 3;
    private int songsBeforeNews = 5;
    private boolean includeSingAlongs = false;
    private boolean skipSplash = false;
    
    // Saints Radio station inclusions
    private boolean includeKrunch = true;
    private boolean includeKrhyme = true;
    private boolean includeMix = true;
    private boolean includeGenx = true;

    public static final String PREFS_NAME = "SaintsRadioPrefs";
    private static final String KEY_COMMERCIALS = "commercialsPerSong";
    private static final String KEY_NEWS = "songsBeforeNews";
    private static final String KEY_SING_ALONGS = "includeSingAlongs";
    public static final String KEY_SKIP_SPLASH = "skipSplash";
    
    private static final String KEY_INCLUDE_KRUNCH = "includeKrunch";
    private static final String KEY_INCLUDE_KRHYME = "includeKrhyme";
    private static final String KEY_INCLUDE_MIX = "includeMix";
    private static final String KEY_INCLUDE_GENX = "includeGenx";

    public static final String Broadcast_START_SAINTS_RADIO = "com.example.saintsrowradio.StartSaintsRadio";
    public static final String Broadcast_START_KRUNCH_RADIO = "com.example.saintsrowradio.StartKrunchRadio";
    public static final String Broadcast_START_KRHYME_RADIO = "com.example.saintsrowradio.StartKrhymeRadio";
    public static final String Broadcast_START_MIX_RADIO = "com.example.saintsrowradio.StartMixRadio";
    public static final String Broadcast_START_GENX_RADIO = "com.example.saintsrowradio.StartGenxRadio";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    initializeMediaController();
                }
            });

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initializeMediaController() {
        if (controllerFuture == null) {
            SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MediaPlayerService.class));
            controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
            controllerFuture.addListener(() -> {
                try {
                    mediaController = controllerFuture.get();
                    // Sync settings to service immediately after connection
                    updateServiceSettings();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, MoreExecutors.directExecutor());
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
        songsBeforeNews = prefs.getInt(KEY_NEWS, 5);
        includeSingAlongs = prefs.getBoolean(KEY_SING_ALONGS, false);
        skipSplash = prefs.getBoolean(KEY_SKIP_SPLASH, false);
        
        includeKrunch = prefs.getBoolean(KEY_INCLUDE_KRUNCH, true);
        includeKrhyme = prefs.getBoolean(KEY_INCLUDE_KRHYME, true);
        includeMix = prefs.getBoolean(KEY_INCLUDE_MIX, true);
        includeGenx = prefs.getBoolean(KEY_INCLUDE_GENX, true);
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COMMERCIALS, commercialsPerSong);
        editor.putInt(KEY_NEWS, songsBeforeNews);
        editor.putBoolean(KEY_SING_ALONGS, includeSingAlongs);
        editor.putBoolean(KEY_SKIP_SPLASH, skipSplash);
        
        editor.putBoolean(KEY_INCLUDE_KRUNCH, includeKrunch);
        editor.putBoolean(KEY_INCLUDE_KRHYME, includeKrhyme);
        editor.putBoolean(KEY_INCLUDE_MIX, includeMix);
        editor.putBoolean(KEY_INCLUDE_GENX, includeGenx);
        
        editor.apply();
    }

    private void updateServiceSettings() {
        if (mediaController != null) {
            Bundle args = new Bundle();
            args.putInt(KEY_COMMERCIALS, commercialsPerSong);
            args.putInt(KEY_NEWS, songsBeforeNews);
            args.putBoolean(KEY_SING_ALONGS, includeSingAlongs);
            
            args.putBoolean(KEY_INCLUDE_KRUNCH, includeKrunch);
            args.putBoolean(KEY_INCLUDE_KRHYME, includeKrhyme);
            args.putBoolean(KEY_INCLUDE_MIX, includeMix);
            args.putBoolean(KEY_INCLUDE_GENX, includeGenx);
            
            mediaController.sendCustomCommand(new SessionCommand("ACTION_UPDATE_SETTINGS", Bundle.EMPTY), args);
        }
    }

    private void showRotationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);
        scrollView.addView(layout);

        // Commercials Slider
        final TextView commLabel = new TextView(this);
        commLabel.setText("Commercials after each song: " + commercialsPerSong);
        commLabel.setPadding(0, 20, 0, 0);
        layout.addView(commLabel);

        final Slider commSlider = new Slider(this);
        commSlider.setValueFrom(0f);
        commSlider.setValueTo(5f);
        commSlider.setStepSize(1f);
        commSlider.setValue((float) commercialsPerSong);
        commSlider.addOnChangeListener((slider, value, fromUser) -> commLabel.setText("Commercials after each song: " + (int)value));
        layout.addView(commSlider);

        // News Slider
        final TextView newsLabel = new TextView(this);
        newsLabel.setText("Songs before news: " + songsBeforeNews);
        newsLabel.setPadding(0, 40, 0, 0);
        layout.addView(newsLabel);

        final Slider newsSlider = new Slider(this);
        newsSlider.setValueFrom(0f);
        newsSlider.setValueTo(10f);
        newsSlider.setStepSize(1f);
        newsSlider.setValue((float) songsBeforeNews);
        newsSlider.addOnChangeListener((slider, value, fromUser) -> newsLabel.setText("Songs before news: " + (int)value));
        layout.addView(newsSlider);

        // Get primary color from theme to match sliders and menu
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;
        ColorStateList checkboxTint = ColorStateList.valueOf(colorPrimary);

        // Sing Along Label
        final TextView singAlongLabel = new TextView(this);
        singAlongLabel.setText("Include Sing Alongs");
        singAlongLabel.setPadding(0, 40, 0, 0);
        layout.addView(singAlongLabel);

        // Sing Along Checkbox
        final CheckBox singAlongCheckbox = new CheckBox(this);
        singAlongCheckbox.setChecked(includeSingAlongs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            singAlongCheckbox.setButtonTintList(checkboxTint);
        }
        layout.addView(singAlongCheckbox);

        // Skip Splash Label
        final TextView skipSplashLabel = new TextView(this);
        skipSplashLabel.setText(R.string.action_skip_splash);
        skipSplashLabel.setPadding(0, 40, 0, 0);
        layout.addView(skipSplashLabel);

        // Skip Splash Checkbox
        final CheckBox skipSplashCheckbox = new CheckBox(this);
        skipSplashCheckbox.setChecked(skipSplash);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            skipSplashCheckbox.setButtonTintList(checkboxTint);
        }
        layout.addView(skipSplashCheckbox);
        
        // Saints Radio Station Inclusions
        final TextView saintsLabel = new TextView(this);
        saintsLabel.setText(R.string.saints_radio_stations);
        saintsLabel.setPadding(0, 40, 0, 0);
        layout.addView(saintsLabel);
        
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(2);
        gridLayout.setPadding(0, 10, 0, 0);
        
        final CheckBox krunchCheck = createStationCheckbox(getString(R.string.station_krunch), includeKrunch, checkboxTint);
        final CheckBox krhymeCheck = createStationCheckbox(getString(R.string.station_krhyme), includeKrhyme, checkboxTint);
        final CheckBox mixCheck = createStationCheckbox(getString(R.string.station_mix), includeMix, checkboxTint);
        final CheckBox genxCheck = createStationCheckbox(getString(R.string.station_genx), includeGenx, checkboxTint);
        
        gridLayout.addView(krunchCheck);
        gridLayout.addView(krhymeCheck);
        gridLayout.addView(mixCheck);
        gridLayout.addView(genxCheck);
        
        layout.addView(gridLayout);

        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Validation: at least 2 stations
            int count = 0;
            if (krunchCheck.isChecked()) count++;
            if (krhymeCheck.isChecked()) count++;
            if (mixCheck.isChecked()) count++;
            if (genxCheck.isChecked()) count++;
            
            if (count < 2) {
                Toast.makeText(this, "Please select at least 2 stations for Saints Radio", Toast.LENGTH_LONG).show();
                showRotationSettingsDialog(); 
                return;
            }
            
            commercialsPerSong = (int) commSlider.getValue();
            songsBeforeNews = (int) newsSlider.getValue();
            includeSingAlongs = singAlongCheckbox.isChecked();
            skipSplash = skipSplashCheckbox.isChecked();
            
            includeKrunch = krunchCheck.isChecked();
            includeKrhyme = krhymeCheck.isChecked();
            includeMix = mixCheck.isChecked();
            includeGenx = genxCheck.isChecked();
            
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cb.setButtonTintList(tint);
        }
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
                sendBroadcast(new Intent(broadcastAction));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            initializeMediaController();
        } else {
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
    }
}
