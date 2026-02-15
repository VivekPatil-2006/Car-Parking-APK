package com.example.admincarparking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText editTextOtp;
    private Button btnVerify;
    private ProgressBar progressBar;
    private String generatedOtp, mobileNumber;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        editTextOtp = findViewById(R.id.editTextOtp);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBar);

        generatedOtp = getIntent().getStringExtra("generatedOtp");
        mobileNumber = getIntent().getStringExtra("mobile");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnVerify.setOnClickListener(view -> {
            String otp = editTextOtp.getText().toString().trim();
            if (otp.length() == 6) {
                verifyOtp(otp);
            } else {
                Toast.makeText(OtpVerificationActivity.this, "Enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp(String enteredOtp) {
        progressBar.setVisibility(View.VISIBLE); // Show ProgressBar
        btnVerify.setEnabled(false); // Disable button during verification

        if (enteredOtp.equals(generatedOtp)) {
            getSharedPreferences("UserSession", MODE_PRIVATE)
                    .edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("mobileNumber", mobileNumber)
                    .apply();

            addCustomerToFirestore();
        } else {
            progressBar.setVisibility(View.GONE); // Hide ProgressBar
            btnVerify.setEnabled(true); // Re-enable button
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCustomerToFirestore() {
        Map<String, Object> ownerData = new HashMap<>();
        ownerData.put("phone", mobileNumber);
        ownerData.put("name", "");
        ownerData.put("location", "");
        ownerData.put("email", "");

        db.collection("Owner").document(mobileNumber)
                .set(ownerData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE); // Hide ProgressBar
                    Toast.makeText(this, "Verification Successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(OtpVerificationActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE); // Hide ProgressBar
                    btnVerify.setEnabled(true); // Re-enable button
                    Toast.makeText(this, "Failed to add customer.", Toast.LENGTH_SHORT).show();
                });
    }
}
