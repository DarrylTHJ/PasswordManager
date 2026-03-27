package com.example.passwordmanager;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditActivity extends AppCompatActivity {

    private EditText etSiteName, etUsername, etPassword, etNotes;
    private DatabaseHelper databaseHelper;
    private String currentEntryId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        etSiteName = findViewById(R.id.etSiteName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etNotes = findViewById(R.id.etNotes);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnDelete = findViewById(R.id.btnDelete);
        Button btnCopy = findViewById(R.id.btnCopy);
        Button btnGenerate = findViewById(R.id.btnGenerate);
        Button btnBack = findViewById(R.id.btnBack);
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);

        databaseHelper = new DatabaseHelper(this);

        // --- FEATURE: Back Button ---
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // --- FEATURE: Password Visibility Toggle ---
        cbShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        // --- FEATURE: Copy to Clipboard ---
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String passwordToCopy = etPassword.getText().toString();
                if (!passwordToCopy.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied Password", passwordToCopy);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(AddEditActivity.this, "Password copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // --- FEATURE: Live Password Strength Checker ---
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- FEATURE: AI Password Generator ---
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAIPromptDialog();
            }
        });

        // Load existing data if EDITING
        Intent intent = getIntent();
        if (intent.hasExtra("ENTRY_ID")) {
            currentEntryId = intent.getStringExtra("ENTRY_ID");
            etSiteName.setText(intent.getStringExtra("ENTRY_SITE"));
            etUsername.setText(intent.getStringExtra("ENTRY_USERNAME"));

            String decryptedPassword = EncryptionUtils.decrypt(intent.getStringExtra("ENTRY_PASSWORD"));
            etPassword.setText(decryptedPassword);

            if(intent.hasExtra("ENTRY_NOTES")) {
                etNotes.setText(intent.getStringExtra("ENTRY_NOTES"));
            }

            btnSave.setText("Update Entry");
            btnDelete.setVisibility(View.VISIBLE);
        }

        // Handle Save / Update
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String site = etSiteName.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String notes = etNotes.getText().toString().trim();

                if (site.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(AddEditActivity.this, "Please fill required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                String encryptedPassword = EncryptionUtils.encrypt(password);

                if (currentEntryId == null) {
                    boolean isInserted = databaseHelper.insertEntry(site, username, encryptedPassword, notes);
                    if (isInserted) {
                        Toast.makeText(AddEditActivity.this, "Saved Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditActivity.this, "Error saving data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    boolean isUpdated = databaseHelper.updateEntry(currentEntryId, site, username, encryptedPassword, notes);
                    if (isUpdated) {
                        Toast.makeText(AddEditActivity.this, "Updated Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditActivity.this, "Error updating data", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Handle Delete
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer deletedRows = databaseHelper.deleteEntry(currentEntryId);
                if (deletedRows > 0) {
                    Toast.makeText(AddEditActivity.this, "Entry Deleted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void checkPasswordStrength(String password) {
        TextView tvStrength = findViewById(R.id.tvPasswordStrength);

        if (password == null || password.isEmpty()) {
            tvStrength.setVisibility(View.GONE);
            return;
        }

        tvStrength.setVisibility(View.VISIBLE);
        int score = 0;

        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++;

        switch (score) {
            case 0:
            case 1:
                tvStrength.setText("Strength: Weak");
                tvStrength.setTextColor(android.graphics.Color.parseColor("#F44336"));
                break;
            case 2:
                tvStrength.setText("Strength: Fair");
                tvStrength.setTextColor(android.graphics.Color.parseColor("#FF9800"));
                break;
            case 3:
                tvStrength.setText("Strength: Good");
                tvStrength.setTextColor(android.graphics.Color.parseColor("#2196F3"));
                break;
            case 4:
                tvStrength.setText("Strength: Strong");
                tvStrength.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                break;
        }
    }

    private void showAIPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("AI Password Generator");
        builder.setMessage("What should the password be about?");

        final EditText input = new EditText(this);
        input.setHint("e.g., my dog Tim");

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(layout);

        builder.setPositiveButton("Generate", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userPrompt = input.getText().toString().trim();
                if (!userPrompt.isEmpty()) {
                    etPassword.setText("Generating...");
                    callLLMAPI(userPrompt);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void callLLMAPI(String userContext) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String generatedPassword = "";
            try {
                String apiKey = "INSERT AI KEY HERE";
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String safeContext = userContext.replace("\"", "").replace("\\", "");
                String systemPrompt = "You are a secure password generator. Create a strong, 12-16 character password incorporating the user's concept: '" + safeContext + "'. You MUST strictly include at least two numbers and two special characters (e.g., !@#$%^&*). Mix uppercase and lowercase letters. Return ONLY the raw password string with absolutely no extra text, quotes, or markdown.";

                String jsonInputString = "{\"contents\": [{\"parts\":[{\"text\": \"" + systemPrompt + "\"}]}]}";

                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                java.io.InputStream stream;
                if (responseCode >= 200 && responseCode <= 299) {
                    stream = conn.getInputStream();
                } else {
                    stream = conn.getErrorStream();
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                if (responseCode >= 200 && responseCode <= 299) {
                    JSONObject jsonObject = new JSONObject(response.toString());
                    generatedPassword = jsonObject.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text").trim();
                } else {
                    generatedPassword = "API Error " + responseCode + ": " + response.toString();
                }

            } catch (Exception e) {
                e.printStackTrace();
                generatedPassword = "App Error: " + e.getMessage();
            }

            final String finalPassword = generatedPassword;
            handler.post(() -> {
                etPassword.setText(finalPassword);
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);
                if (cbShowPassword != null) cbShowPassword.setChecked(true);
            });
        });
    }
}