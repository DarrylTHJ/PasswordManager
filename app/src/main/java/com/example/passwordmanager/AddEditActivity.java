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
        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);

        databaseHelper = new DatabaseHelper(this);

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
}