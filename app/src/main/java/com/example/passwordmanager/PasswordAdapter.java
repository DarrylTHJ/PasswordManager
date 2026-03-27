package com.example.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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

        // 1. Extract raw data from database
        String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
        String siteName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SITE));
        String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
        String encryptedPassword = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD));

        // Safely extract notes in case a search query skips it
        String notes = "";
        int notesIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_NOTES);
        if(notesIndex != -1) {
            notes = cursor.getString(notesIndex);
        }

        // 2. Decrypt the password for the Quick Access Bar
        String decryptedPassword = EncryptionUtils.decrypt(encryptedPassword);

        // 3. Set the text
        holder.tvRowId.setText(id);
        holder.tvRowSiteName.setText(siteName);
        holder.etQuickPassword.setText(decryptedPassword);

        // --- BUTTON 1: Edit ---
        final String finalNotes = notes;
        holder.btnEditRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AddEditActivity.class);
                intent.putExtra("ENTRY_ID", id);
                intent.putExtra("ENTRY_SITE", siteName);
                intent.putExtra("ENTRY_USERNAME", username);
                intent.putExtra("ENTRY_PASSWORD", encryptedPassword); // Pass the encrypted one!
                intent.putExtra("ENTRY_NOTES", finalNotes);
                context.startActivity(intent);
            }
        });

        // --- BUTTON 2: Show Toggle ---
        holder.cbQuickShow.setOnCheckedChangeListener(null);
        holder.cbQuickShow.setChecked(false); // Default to hidden dots
        holder.etQuickPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        holder.cbQuickShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    holder.etQuickPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    holder.etQuickPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        // --- BUTTON 3: Quick Copy ---
        holder.btnQuickCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Password", decryptedPassword);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

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
        Button btnEditRow, btnQuickCopy;
        EditText etQuickPassword;
        CheckBox cbQuickShow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowId = itemView.findViewById(R.id.tvRowId);
            tvRowSiteName = itemView.findViewById(R.id.tvRowSiteName);
            btnEditRow = itemView.findViewById(R.id.btnEditRow);
            etQuickPassword = itemView.findViewById(R.id.etQuickPassword);
            cbQuickShow = itemView.findViewById(R.id.cbQuickShow);
            btnQuickCopy = itemView.findViewById(R.id.btnQuickCopy);
        }
    }
}