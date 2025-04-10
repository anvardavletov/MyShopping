package tm.davletov.myshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class InstructionActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_instruction);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        viewPager.setAdapter(new InstructionAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {}).attach();

        findViewById(R.id.btn_finish).setOnClickListener(v -> {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("first_run", false).apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}