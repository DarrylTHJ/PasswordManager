package com.example.passwordmanager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.ViewHolder> {
    private Context context;
    private Cursor cursor;

    public PasswordAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_password, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        // Extract all data from the database for this row
        String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
        String siteName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SITE));
        String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
        String password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD));

        // Put the visible data into the text views
        holder.tvRowId.setText(id);
        holder.tvRowSiteName.setText(siteName);

        // Make the entire row clickable
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Package the data and send it to AddEditActivity
                Intent intent = new Intent(context, AddEditActivity.class);
                intent.putExtra("ENTRY_ID", id);
                intent.putExtra("ENTRY_SITE", siteName);
                intent.putExtra("ENTRY_USERNAME", username);
                intent.putExtra("ENTRY_PASSWORD", password);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    // This method helps refresh the list when a new item is added
    public void swapCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        if (newCursor != null) {
            notifyDataSetChanged();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRowId, tvRowSiteName;

        // This constructor requires a View (itemView), and passes it to the parent class using super(itemView)
        public ViewHolder(@NonNull View itemView) {
            super(itemView); // <-- This specific line fixes your error!
            tvRowId = itemView.findViewById(R.id.tvRowId);
            tvRowSiteName = itemView.findViewById(R.id.tvRowSiteName);
        }
    }
}