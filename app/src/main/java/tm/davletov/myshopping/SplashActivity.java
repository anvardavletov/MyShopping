package tm.davletov.myshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем тему до setContentView
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        int theme = prefs.getInt("theme", 0); // 0 = THEME_BASE
        // Отладка: выводим текущую тему в лог
        Log.d("MyListsActivity", "Current theme: " + theme);

        switch (theme) {
            case 0: // THEME_BASE
                setTheme(R.style.AppTheme_Green);
                break;
            case 1: // THEME_ORANGE
                setTheme(R.style.AppTheme_Orange);
                break;
            case 2: // THEME_DARK
                setTheme(R.style.AppTheme_Dark);
                break;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new android.os.Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2000); // Задержка 2 секунды
    }
}