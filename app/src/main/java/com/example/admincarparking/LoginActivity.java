package com.example.admincarparking;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextMobile;
    private Button btnGetCode;
    private String generatedOtp;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        editTextMobile = findViewById(R.id.editTextMobile);
        btnGetCode = findViewById(R.id.btnGetCode);

        editTextMobile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnGetCode.setEnabled(s.length() == 10);
                btnGetCode.setBackgroundTintList(ContextCompat.getColorStateList(
                        LoginActivity.this, s.length() == 10 ? R.color.blue : R.color.gray));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnGetCode.setOnClickListener(view -> {
            String mobile = "+91" + editTextMobile.getText().toString().trim();
            generatedOtp = generateOtp();
            checkAndRequestSmsPermission(mobile, generatedOtp);
        });
    }

    private void savePhoneNumberToSharedPreferences(String phoneNumber) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("phoneNumber", phoneNumber);
        editor.apply();
    }

    private String generateOtp() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private void checkAndRequestSmsPermission(String mobile, String otp) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                showPermissionExplanationDialog(mobile, otp);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
            }
        } else {
            sendOtpToUser(mobile, otp);
        }
    }

    private void showPermissionExplanationDialog(String mobile, String otp) {
        new AlertDialog.Builder(this)
                .setTitle("SMS Permission Required")
                .setMessage("This app needs SMS permission to send OTPs. Please allow it in Settings.")
                .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(
                        LoginActivity.this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE
                ))
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this, "SMS permission denied. Cannot send OTP.", Toast.LENGTH_SHORT).show())
                .show();
    }

    private void sendOtpToUser(String mobile, String otp) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "Your OTP for registration is: " + otp;
            smsManager.sendTextMessage(mobile, null, message, null, null);
            Toast.makeText(this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send OTP.", Toast.LENGTH_SHORT).show();
        }

        savePhoneNumberToSharedPreferences(mobile);

        Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
        intent.putExtra("generatedOtp", otp);
        intent.putExtra("mobile", mobile);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendOtpToUser("+91" + editTextMobile.getText().toString().trim(), generatedOtp);
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("SMS permission is required to send OTPs. Please enable it in Settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this, "Cannot send OTP without SMS permission.", Toast.LENGTH_SHORT).show())
                .show();
    }
}
