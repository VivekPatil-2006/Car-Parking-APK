package com.example.admincarparking;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AccountFragment extends Fragment {

    private EditText etUsername, etEmail, etLocation, etPhone;
    private Button btnEditProfile, btnFetchLocation;
    private FirebaseFirestore db;
    private String phoneNumber;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        phoneNumber = sharedPreferences.getString("phoneNumber", "");

        etUsername = view.findViewById(R.id.etUsername);
        etEmail = view.findViewById(R.id.etEmail);
        etLocation = view.findViewById(R.id.etLocation);
        etPhone = view.findViewById(R.id.etPhone);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnFetchLocation = view.findViewById(R.id.btnFetchLocation);

        etPhone.setText(phoneNumber);
        etPhone.setEnabled(false);

        fetchUserDetails();

        // Check if location is already set
        String storedLocation = sharedPreferences.getString("ownerLocation", "");
        if (!storedLocation.isEmpty()) {
            etLocation.setText(storedLocation);
            etLocation.setEnabled(false); // Disable editing if location is already set
            btnFetchLocation.setEnabled(false); // Disable fetching location again
        }

        btnFetchLocation.setOnClickListener(v -> checkGPSAndFetchLocation());
        btnEditProfile.setOnClickListener(v -> updateUserProfile());

        return view;
    }

    private void fetchUserDetails() {
        if (phoneNumber.isEmpty()) {
            Toast.makeText(getActivity(), "No phone number found!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("Owner").document(phoneNumber);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etUsername.setText(documentSnapshot.getString("name"));
                etEmail.setText(documentSnapshot.getString("email"));
                etLocation.setText(documentSnapshot.getString("location"));

                if (documentSnapshot.contains("location") && !documentSnapshot.getString("location").isEmpty()) {
                    etLocation.setEnabled(false);
                    btnFetchLocation.setEnabled(false);
                }
            } else {
                Toast.makeText(getActivity(), "User not found!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getActivity(), "Failed to load user details!", Toast.LENGTH_SHORT).show()
        );
    }

    private void checkGPSAndFetchLocation() {
        LocationManager locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getActivity(), "Please enable GPS", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                String ownerLocation = location.getLatitude() + "," + location.getLongitude();

                etLocation.setText(ownerLocation);
                etLocation.setEnabled(false); // Disable editing
                btnFetchLocation.setEnabled(false); // Disable fetching location again

                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ownerLocation", ownerLocation);
                editor.apply();
            } else {
                Toast.makeText(getActivity(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserProfile() {
        String name = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || location.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill in all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("location", location);
        userData.put("phone", phoneNumber);

        db.collection("Owner").document(phoneNumber).set(userData)
                .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to update profile!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(getActivity(), "Location permission denied!", Toast.LENGTH_SHORT).show();
        }
    }
}
