package tm.davletov.myshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем дефолтную тему
        setTheme(R.style.AppTheme_Green);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Инициализируем CurrencyManager
        CurrencyManager currencyManager = new CurrencyManager(this, new CurrencyManager.CurrencyLoadListener() {
            @Override
            public void onCurrencyReady(String newSymbol) {
                Log.d("SplashActivity", "Currency set: " + newSymbol);
                // Переход в MainActivity после установки валюты
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }, SPLASH_DELAY);
            }

            @Override
            public void onCurrencyLoadFailed() {
                Log.e("SplashActivity", "Failed to load currency, using default");
                // Переход в MainActivity даже при ошибке
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }, SPLASH_DELAY);
            }
        });

        // Загружаем тему асинхронно (необязательно, но для оптимизации)
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            int theme = prefs.getInt("theme", 0);
            Log.d("SplashActivity", "Current theme: " + theme);
        }).start();
    }
}