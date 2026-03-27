package com.example.passwordmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etPin;
    private TextView tvInstruction;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_PIN = "master_pin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPin = findViewById(R.id.etPin);
        tvInstruction = findViewById(R.id.tvPinInstruction);
        Button btnSubmitPin = findViewById(R.id.btnSubmitPin);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedPin = sharedPreferences.getString(KEY_PIN, null);

        // Change text based on whether a PIN is already set
        if (savedPin == null) {
            tvInstruction.setText("Create a 4-Digit PIN");
        } else {
            tvInstruction.setText("Enter your 4-Digit PIN");
        }

        btnSubmitPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPin = etPin.getText().toString();

                if (inputPin.length() != 4) {
                    Toast.makeText(LoginActivity.this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (savedPin == null) {
                    // First time setup
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_PIN, inputPin);
                    editor.apply();
                    Toast.makeText(LoginActivity.this, "PIN Saved!", Toast.LENGTH_SHORT).show();
                    goToMain();
                } else {
                    // Returning user login
                    if (inputPin.equals(savedPin)) {
                        goToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        etPin.setText(""); // clear the field
                    }
                }
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}