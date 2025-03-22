package com.chaitany.carbonview;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class UploadsAdapter extends RecyclerView.Adapter<UploadsAdapter.ViewHolder> {

    private final Context context;
    private final List<UploadItem> uploadList;

    public UploadsAdapter(Context context, List<UploadItem> uploadList) {
        this.context = context;
        this.uploadList = uploadList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item_recent_uploads, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UploadItem uploadItem = uploadList.get(position);

        holder.fileNameText.setText(uploadItem.getFileName());
        holder.uploadDateText.setText("Uploaded on: " + uploadItem.getDate());

        // Analyze button click event
        holder.analyzeButton.setOnClickListener(v -> {
            // Fetch the file URL from the upload item
            String fileUrl = uploadItem.getFileUrl();

            // Launch AnalyzeActivity and pass the file URL
            Intent intent = new Intent(context, AnalyzeActivity.class);
            intent.putExtra("fileUrl", fileUrl);
            context.startActivity(intent);
        });

        // Delete button click event
        holder.deleteButton.setOnClickListener(v -> deleteFile(uploadItem, position));
    }

    @Override
    public int getItemCount() {
        return uploadList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, uploadDateText;
        MaterialButton analyzeButton, deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            uploadDateText = itemView.findViewById(R.id.dateText);
            analyzeButton = itemView.findViewById(R.id.analyzeButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    private void deleteFile(UploadItem uploadItem, int position) {
        // Get reference to the user's uploads in Firebase Realtime Database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users")
                .child(uploadItem.getMobile()) // Assuming "userId" is the unique identifier
                .child("uploads");

        // Get reference to the file in Firebase Storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(uploadItem.getFileUrl());

        // Delete the file from Firebase Storage
        storageReference.delete().addOnSuccessListener(aVoid -> {
            // If file is successfully deleted from Storage, proceed to delete from Firebase Realtime Database
            databaseReference.child(uploadItem.getFileName()).removeValue()
                    .addOnSuccessListener(aVoid1 -> {
                        // Remove the item from the uploadList and notify the adapter
                        uploadList.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "File and data deleted successfully", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        // If failed to remove from database, show an error message
                        Toast.makeText(context, "Failed to delete data from the database", Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            // If failed to delete the file from Storage, show an error message
            Toast.makeText(context, "Failed to delete file from storage", Toast.LENGTH_SHORT).show();
        });
    }


}
