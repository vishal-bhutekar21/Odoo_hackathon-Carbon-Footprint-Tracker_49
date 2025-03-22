package com.chaitany.carbonview;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class AddData extends AppCompatActivity {
    private static final int FILE_SELECT_CODE = 1;

    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private FirebaseAuth auth;

    private Dialog progressDialog;
    private MaterialTextView progressText;

    private RecyclerView uploadsRecyclerView;
    private UploadsAdapter uploadAdapter;
    private MaterialCardView materialCardView;
    private List<UploadItem> uploadList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_data);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add Data"); // Explicitly set the title
        }

        // Set status bar color to match the Toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
            // Set status bar icons to light (white) if the background is dark
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        uploadsRecyclerView = findViewById(R.id.uploadsRecyclerView);
        uploadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize uploadList before setting adapter
        uploadList = new ArrayList<>();
        uploadAdapter = new UploadsAdapter(this, uploadList);
        uploadsRecyclerView.setAdapter(uploadAdapter);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("uploads");
        storageReference = FirebaseStorage.getInstance().getReference("uploads").child(userId);

        materialCardView = findViewById(R.id.manualEntryCard);
        MaterialCardView fileUploadCard = findViewById(R.id.fileUploadCard);

        fileUploadCard.setOnClickListener(v -> openFileChooser());

        setupProgressDialog();

        // Call after initializing adapter
        loadUploadedFiles();

        materialCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AddData.this, EstimationGrid.class);
                startActivity(i);
                finish();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupProgressDialog() {
        progressDialog = new Dialog(this);
        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog.setContentView(R.layout.dialog_upload_progress);
        progressDialog.setCancelable(false);
        progressText = progressDialog.findViewById(R.id.progressText);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");  // Base text type filter
        String[] mimeTypes = {"text/csv", "text/comma-separated-values", "text/plain"};  // Allowed MIME types
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        // Create chooser with explicit title to guide user
        Intent chooser = Intent.createChooser(intent, "Select CSV File");
        startActivityForResult(chooser, FILE_SELECT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                // Get file name with extension
                String fileName = getFileName(fileUri);

                // Validate CSV extension
                if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                    uploadFileToFirebase(fileUri);
                } else {
                    // Show error message for invalid file type
                    Toast.makeText(this, "Please select a CSV file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Helper method to get file name from URI
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadFileToFirebase(Uri fileUri) {
        String userId = auth.getCurrentUser().getUid();
        DatabaseReference userUploadsReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("uploads");

        // Get the current month and year
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        String currentMonth = monthFormat.format(new Date());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        String currentYear = yearFormat.format(new Date());

        // Generate a random number between 1 and 20
        int randomNum = new Random().nextInt(20) + 1;

        // Construct the file name with random number
        String fileName = "Data" + randomNum + "_" + currentMonth + "_" + currentYear;

        StorageReference fileRef = storageReference.child(fileName);

        // Create metadata with user ID
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("userId", userId)
                .build();

        progressDialog.show();

        // Upload the file to Firebase Storage
        fileRef.putFile(fileUri, metadata)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String fileUrl = uri.toString();
                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                    // Create an UploadItem object to save in the database
                    UploadItem uploadItem = new UploadItem(fileName, date, fileUrl, userId);

                    // Save the metadata in Firebase Realtime Database
                    userUploadsReference.push().setValue(uploadItem)
                            .addOnSuccessListener(aVoid -> {
                                progressDialog.dismiss();
                                Toast.makeText(AddData.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AddData.this, "Database update failed", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            });

                })).addOnFailureListener(e -> {
                    Toast.makeText(AddData.this, "File upload failed", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }

    private void loadUploadedFiles() {
        String userId = auth.getCurrentUser().getUid();
        StorageReference storageRef = storageReference; // Your root storage reference

        storageRef.listAll()
                .addOnSuccessListener(listResult -> {
                    uploadList.clear();
                    List<StorageReference> items = listResult.getItems();

                    if (items.isEmpty()) {
                        Toast.makeText(AddData.this, "No files found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AtomicInteger processedCount = new AtomicInteger(0);
                    for (StorageReference itemRef : items) {
                        itemRef.getMetadata().addOnSuccessListener(metadata -> {
                            // Check if the file belongs to current user
                            String fileUserId = metadata.getCustomMetadata("userId");
                            if (userId.equals(fileUserId)) {
                                itemRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    // Create date from metadata
                                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                            .format(new Date(metadata.getCreationTimeMillis()));

                                    uploadList.add(new UploadItem(
                                            itemRef.getName(),
                                            date,
                                            uri.toString(),
                                            userId
                                    ));

                                    // Update adapter when all files processed
                                    if (processedCount.incrementAndGet() == items.size()) {
                                        uploadAdapter.notifyDataSetChanged();
                                    }
                                });
                            } else {
                                if (processedCount.incrementAndGet() == items.size()) {
                                    uploadAdapter.notifyDataSetChanged();
                                }
                            }
                        }).addOnFailureListener(e -> {
                            processedCount.incrementAndGet();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddData.this, "Failed to load files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}