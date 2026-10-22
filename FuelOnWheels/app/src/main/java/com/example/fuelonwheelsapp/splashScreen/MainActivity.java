package com.example.fuelonwheelsapp.splashScreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.example.fuelonwheelsapp.BuildConfig;
import com.example.fuelonwheelsapp.R;
import com.example.fuelonwheelsapp.signIn.SignInActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        // Remove Status bar / Fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // Safely initialize AppCheck & OSMDroid
        try {
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue("FuelOnWheelsApp/1.0 (support@fuelonwheels.org)");
            FirebaseApp.initializeApp(/*context=*/ this);
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance());
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "AppCheck init skipped or failed: " + e.getMessage());
        }

        startLuxuryEntranceAnimations();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        MainActivity.this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(intent, options.toBundle());
                finish();
            }
        }, SPLASH_DURATION);
    }

    private void startLuxuryEntranceAnimations() {
        View splashAura = findViewById(R.id.splash_aura);
        View logoCard = findViewById(R.id.logo_card);
        View brandInfo = findViewById(R.id.brand_info_layout);
        View bottomContainer = findViewById(R.id.bottom_container);

        if (splashAura != null) {
            splashAura.setAlpha(0f);
            splashAura.setScaleX(0.7f);
            splashAura.setScaleY(0.7f);
            splashAura.animate()
                    .alpha(0.85f)
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(1200)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        if (logoCard != null) {
            logoCard.setAlpha(0f);
            logoCard.setScaleX(0.6f);
            logoCard.setScaleY(0.6f);
            logoCard.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }

        if (brandInfo != null) {
            brandInfo.setAlpha(0f);
            brandInfo.setTranslationY(40f);
            brandInfo.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(350)
                    .setDuration(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (bottomContainer != null) {
            bottomContainer.setAlpha(0f);
            bottomContainer.setTranslationY(25f);
            bottomContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(650)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
}
