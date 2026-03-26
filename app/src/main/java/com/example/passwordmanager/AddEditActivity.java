package com.example.passwordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddEditActivity extends AppCompatActivity {

    private EditText etSiteName, etUsername, etPassword;
    private DatabaseHelper databaseHelper;
    private String currentEntryId = null; // Will stay null if we are ADDING a new entry

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        etSiteName = findViewById(R.id.etSiteName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnDelete = findViewById(R.id.btnDelete);

        databaseHelper = new DatabaseHelper(this);

        // Check if we arrived here by tapping an existing list item
        Intent intent = getIntent();
        if (intent.hasExtra("ENTRY_ID")) {
            // EDIT MODE: Populate fields and show Delete button
            currentEntryId = intent.getStringExtra("ENTRY_ID");
            etSiteName.setText(intent.getStringExtra("ENTRY_SITE"));
            etUsername.setText(intent.getStringExtra("ENTRY_USERNAME"));
            String decryptedPassword = EncryptionUtils.decrypt(intent.getStringExtra("ENTRY_PASSWORD"));
            etPassword.setText(decryptedPassword);

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
                String encryptedPassword = EncryptionUtils.encrypt(password);

                if (site.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(AddEditActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentEntryId == null) {
                    // ADD NEW
                    boolean isInserted = databaseHelper.insertEntry(site, username, encryptedPassword);
                    if (isInserted) {
                        Toast.makeText(AddEditActivity.this, "Saved Successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddEditActivity.this, "Error saving data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // UPDATE EXISTING
                    boolean isUpdated = databaseHelper.updateEntry(currentEntryId, site, username, encryptedPassword);
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
                } else {
                    Toast.makeText(AddEditActivity.this, "Error deleting data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}