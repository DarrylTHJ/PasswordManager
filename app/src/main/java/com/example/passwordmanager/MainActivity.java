package com.example.passwordmanager;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private PasswordAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Database and UI
        databaseHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewPasswords);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup the + ADD button
        Button btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                startActivity(intent);
            }
        });

        // Setup the Log Out button
        Button btnLogOut = findViewById(R.id.btnLogOut);
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Destroys MainActivity so the user cannot press "Back" to get in
            }
        });

        loadDatabaseData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This ensures the list automatically refreshes when you come back from adding a password
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
}