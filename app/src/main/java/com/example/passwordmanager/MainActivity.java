package com.example.passwordmanager; // Ensure this matches yours!

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private PasswordAdapter adapter;
    private RecyclerView recyclerView;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewPasswords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        etSearch = findViewById(R.id.etSearch);

        Button btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                startActivity(intent);
            }
        });

        Button btnLogOut = findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // --- LIVE SEARCH LOGIC ---
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This triggers every time a letter is typed or deleted
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadDatabaseData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear search and reload all data when coming back from adding/editing
        if (etSearch != null) etSearch.setText("");
        loadDatabaseData();
    }

    private void loadDatabaseData() {
        Cursor cursor = databaseHelper.getAllEntries();
        if (adapter == null) {
            adapter = new PasswordAdapter(this, cursor);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.swapCursor(cursor);
        }
    }

    // Helper method to swap the list data based on the search word
    private void filterList(String keyword) {
        Cursor cursor;
        if (keyword.isEmpty()) {
            cursor = databaseHelper.getAllEntries();
        } else {
            cursor = databaseHelper.searchEntries(keyword);
        }

        if (adapter != null) {
            adapter.swapCursor(cursor);
        }
    }
}