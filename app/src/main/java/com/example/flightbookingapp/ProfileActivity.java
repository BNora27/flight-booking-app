package com.example.flightbookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameTV, emailTV, phoneTV, phoneTypeTV, addressTV;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind views
        usernameTV = findViewById(R.id.profileUsername);
        emailTV = findViewById(R.id.profileEmail);
        phoneTV = findViewById(R.id.profilePhone);
        phoneTypeTV = findViewById(R.id.profilePhoneType);
        addressTV = findViewById(R.id.profileAddress);

        // Load user data from Firestore
        loadUserProfile(currentUser.getUid());

        // Logout
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });

        // Back
        findViewById(R.id.backButton).setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, FlightListActivity.class));
            finish();
        });
    }

    private void loadUserProfile(String uid) {
        DocumentReference userRef = db.collection("Users").document(uid);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usernameTV.setText(documentSnapshot.getString("username"));
                        emailTV.setText(documentSnapshot.getString("email"));
                        phoneTV.setText(documentSnapshot.getString("phone"));
                        phoneTypeTV.setText(documentSnapshot.getString("phoneType"));
                        addressTV.setText(documentSnapshot.getString("address"));
                    } else {
                        Toast.makeText(ProfileActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
