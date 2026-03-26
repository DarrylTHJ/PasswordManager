package com.example.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

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
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // This instantly closes the screen and goes back
            }
        });
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);

        databaseHelper = new DatabaseHelper(this);

        Button btnGenerate = findViewById(R.id.btnGenerate);

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAIPromptDialog();
            }
        });

        // --- FEATURE: Password Visibility Toggle ---
        cbShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Show Password
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    // Hide Password
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                etPassword.setSelection(etPassword.getText().length()); // Keep cursor at the end
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

        // Load existing data if EDITING
        Intent intent = getIntent();
        if (intent.hasExtra("ENTRY_ID")) {
            currentEntryId = intent.getStringExtra("ENTRY_ID");
            etSiteName.setText(intent.getStringExtra("ENTRY_SITE"));
            etUsername.setText(intent.getStringExtra("ENTRY_USERNAME"));

            String decryptedPassword = EncryptionUtils.decrypt(intent.getStringExtra("ENTRY_PASSWORD"));
            etPassword.setText(decryptedPassword);

            // Load Notes (if they exist)
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

    // 1. Show a popup asking the user what kind of password they want
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

    // 2. Make the background network call to the LLM
    private void callLLMAPI(String userContext) {
        // We use an ExecutorService because Android blocks network calls on the main UI thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String generatedPassword = "";
            try {
                // --- SETUP FOR GEMINI API --
                String apiKey = "AIzaSyDVmifbrnY8IvaFjYkEby52Fod0IaBiQ8c";
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // We instruct the LLM to act strictly as a password generator
                String systemPrompt = "You are a secure password generator. Create a strong, 12-16 character password incorporating the user's concept: '" + userContext + "'. You MUST strictly include at least two numbers and two special characters (e.g., !@#$%^&*). Mix uppercase and lowercase letters. Return ONLY the raw password string with absolutely no extra text, quotes, or markdown.";

                String jsonInputString = "{\"contents\": [{\"parts\":[{\"text\": \"" + systemPrompt + "\"}]}]}";

                // Send the request
                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Read the response
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // Parse the JSON to grab just the text
                JSONObject jsonObject = new JSONObject(response.toString());
                generatedPassword = jsonObject.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text").trim();

            } catch (Exception e) {
                e.printStackTrace();
                generatedPassword = "Error: Check Logcat";
            }

            // Send the result back to the main UI thread to update the text box
            final String finalPassword = generatedPassword;
            handler.post(() -> {
                etPassword.setText(finalPassword);
                // Automatically switch input type so the user can see the generated password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);
                if (cbShowPassword != null) cbShowPassword.setChecked(true);
            });
        });
    }



}