package com.example.productiondisplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.productiondisplay.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    // Store credentials accessible for fragments
    public static String userEmail = "";
    public static String userPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String username = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getString("USERNAME", "");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;

        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Intercept gallery navigation to pass credentials bundle
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_gallery) {
                Bundle bundle = new Bundle();
                bundle.putString("email", userEmail);
                bundle.putString("password", userPassword);

                navController.navigate(R.id.nav_gallery, bundle);
                drawer.closeDrawers();
                return true;
            } else {
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled) drawer.closeDrawers();
                return handled;
            }
        });

        if (getIntent() != null && getIntent().hasExtra("email")) {
            userEmail = getIntent().getStringExtra("email");
            userPassword = getIntent().getStringExtra("password");
        }

        // By default show HomeFragment
        navController.navigate(R.id.nav_home);

        querySupportedLanguages();
    }

    private void querySupportedLanguages() {
        try {
            Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
            sendOrderedBroadcast(detailsIntent, null,
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Bundle extras = getResultExtras(true);
                            if (extras != null &&
                                    extras.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                                ArrayList<String> languages = extras.getStringArrayList(
                                        RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                                if (languages != null) {
                                    Toast.makeText(context, "Languages: " + languages, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }, null, RESULT_OK, null, null);
        } catch (Exception e) {
            Toast.makeText(this, "Error querying languages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // Set white background + black text for all menu items
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);

            // Make text black
            SpannableString span = new SpannableString(item.getTitle());
            span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, span.length(), 0);
            item.setTitle(span);
        }

        // Force popup menu background to white
        View decorView = getWindow().getDecorView();
        decorView.post(() -> {
            try {
                Field mPopupField = menu.getClass().getDeclaredField("mPopup");
                mPopupField.setAccessible(true);
                Object menuPopupHelper = mPopupField.get(menu);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                Field mPopup = classPopupHelper.getDeclaredField("mPopup");
                mPopup.setAccessible(true);
                Object popup = mPopup.get(menuPopupHelper);

                if (popup instanceof ListView) {
                    ((ListView) popup).setBackgroundColor(Color.WHITE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_exit) {
            finish();
            return true;
        } else if (id == R.id.action_login) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
