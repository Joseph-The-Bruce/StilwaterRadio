package com.example.saintsrowradio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private VideoView videoView;
    private final List<Integer> logoList = new ArrayList<>();
    private int currentLogoIndex = 0;
    private int stopPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE);
        boolean skipSplash = prefs.getBoolean(MainActivity.KEY_SKIP_SPLASH, false);

        if (skipSplash) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_splash);

        videoView = findViewById(R.id.videoView);
        
        logoList.add(R.raw.logo_thq_hd);
        logoList.add(R.raw.logo_volition_hd);

        if (savedInstanceState != null) {
            currentLogoIndex = savedInstanceState.getInt("currentLogoIndex", 0);
            stopPosition = savedInstanceState.getInt("stopPosition", 0);
        }

        videoView.setOnCompletionListener(mp -> playNextVideo());
        videoView.setOnErrorListener((mp, what, extra) -> {
            playNextVideo();
            return true;
        });

        playVideo(currentLogoIndex);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentLogoIndex", currentLogoIndex);
        if (videoView != null) {
            outState.putInt("stopPosition", videoView.getCurrentPosition());
        }
    }

    private void playVideo(int index) {
        if (index < logoList.size()) {
            String videoPath = "android.resource://" + getPackageName() + "/" + logoList.get(index);
            videoView.setVideoURI(Uri.parse(videoPath));
            if (stopPosition > 0) {
                videoView.seekTo(stopPosition);
                stopPosition = 0;
            }
            videoView.start();
        } else {
            startMainActivity();
        }
    }

    private void playNextVideo() {
        currentLogoIndex++;
        stopPosition = 0;
        playVideo(currentLogoIndex);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
