package com.example.productiondisplay;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button loginButton;
    private ImageView eyeToggle;
    private boolean isPasswordVisible = false;

    private static final String BASE_URL = "https://pds1.iotsignin.com/api/testing/user/get-data";
    private static final String AUTH_TOKEN = "Bearer sb-eba140ab-74bb-44a4-8d92-70a636940def!b1182|it-rt-dev-cri-stjllphr!b68:616d8991-307b-4ab1-be37-7894a8c6db9d$0p0fE2I7w1Ve23-lVSKQF0ka3mKrTVcKPJYELr-i4nE=";

    // Model for user response JSON
    public static class UserResponse {
        private String created_at;
        private String updated_at;
        private String email;
        private String[] roles;
        private String password;

        public String getEmail() { return email; }
        public String getPassword() { return password; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        eyeToggle = findViewById(R.id.eye_button);
        loginButton.setEnabled(false);
    }

    private void setupListeners() {
        TextWatcher fieldWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFields();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        usernameEditText.addTextChangedListener(fieldWatcher);
        passwordEditText.addTextChangedListener(fieldWatcher);

        usernameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                passwordEditText.requestFocus();
                return true;
            }
            return false;
        });

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (loginButton.isEnabled()) {
                    attemptLogin();
                }
                return true;
            }
            return false;
        });

        eyeToggle.setOnClickListener(v -> togglePasswordVisibility());
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void checkFields() {
        boolean fieldsFilled = !usernameEditText.getText().toString().trim().isEmpty() &&
                !passwordEditText.getText().toString().trim().isEmpty();
        loginButton.setEnabled(fieldsFilled);
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        validateCredentials(username, password);
    }

    private void validateCredentials(String username, String password) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL)
                .get()
                .addHeader("Authorization", AUTH_TOKEN)
                .addHeader("User-Name", username)
                .addHeader("User-Pass", password)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                runOnUiThread(() -> {
                    try {
                        UserResponse userResponse = new Gson().fromJson(responseBody, UserResponse.class);
                        if (userResponse != null && userResponse.getEmail() != null) {
                            String resolvedPassword = userResponse.getPassword() != null ?
                                    userResponse.getPassword() : password;

                            // Save username safely in SharedPreferences
                            String usernameEntered = usernameEditText.getText().toString().trim();
                            getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("USERNAME", usernameEntered)
                                    .apply();

                            // Bundle for MainActivity
                            Bundle bundle = new Bundle();
                            bundle.putString("email", userResponse.getEmail());
                            bundle.putString("password", resolvedPassword);

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtras(bundle);
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error processing response", Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "Error processing response: " + responseBody, e);
                    }
                });
            }
        });
    }
}