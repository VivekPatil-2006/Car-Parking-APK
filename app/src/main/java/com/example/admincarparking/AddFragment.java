package com.example.admincarparking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddFragment extends Fragment {

    private EditText etNumFloors;
    private Button btnNext, btnSave;
    private LinearLayout floorContainer;
    private TextView tvExistingData;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String ownerNumber;
    private String ownerLocation;
    private GeoPoint ownerGeoPoint;
    private int numFloors = 0;

    public AddFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        etNumFloors = view.findViewById(R.id.et_num_floors);
        btnNext = view.findViewById(R.id.btn_next);
        btnSave = view.findViewById(R.id.btn_save);
        floorContainer = view.findViewById(R.id.floor_container);
        tvExistingData = view.findViewById(R.id.tv_existing_data);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        ownerNumber = sharedPreferences.getString("phoneNumber", "");
        ownerLocation = sharedPreferences.getString("ownerLocation", "0.0,0.0").replace(" ", "");

        if (ownerNumber.isEmpty()) {
            Toast.makeText(getContext(), "Owner phone number not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return view;
        }

        try {
            String[] latLng = ownerLocation.split(",");
            double latitude = Double.parseDouble(latLng[0]);
            double longitude = Double.parseDouble(latLng[1]);
            ownerGeoPoint = new GeoPoint(latitude, longitude);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Invalid location format.", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize building document
        DocumentReference buildingDoc = db.collection("building").document(ownerLocation);
        Map<String, Object> buildingData = new HashMap<>();
        buildingData.put("ownerPhone", ownerNumber);
        buildingData.put("geoPoint", ownerGeoPoint);
        buildingDoc.set(buildingData, SetOptions.merge());

        fetchExistingParkingDetails();

        btnNext.setOnClickListener(v -> generateFloorInputs());
        btnSave.setOnClickListener(v -> saveParkingDetails());

        return view;
    }

    private void fetchExistingParkingDetails() {
        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        DocumentReference docRef = db.collection("building").document(ownerLocation);
        StringBuilder existingData = new StringBuilder("Existing Floors and Slots:\n");

        docRef.collection("floors").get().addOnSuccessListener(floorDocs -> {
            if (!floorDocs.isEmpty()) {
                List<String> floorNames = new ArrayList<>();

                // Collect floor names
                for (QueryDocumentSnapshot floorDoc : floorDocs) {
                    floorNames.add(floorDoc.getId());
                }

                // Sort floors numerically
                Collections.sort(floorNames, (f1, f2) -> {
                    int num1 = extractFloorNumber(f1);
                    int num2 = extractFloorNumber(f2);
                    return Integer.compare(num1, num2);
                });

                // Track completed async operations
                final int totalFloors = floorNames.size();
                final int[] completed = {0};
                Map<String, Integer> floorSlotCounts = new HashMap<>();

                for (String floorName : floorNames) {
                    docRef.collection("floors").document(floorName)
                            .collection("slots").get()
                            .addOnSuccessListener(slotDocs -> {
                                floorSlotCounts.put(floorName, slotDocs.size());
                                completed[0]++;

                                // Update UI when all data is ready
                                if (completed[0] == totalFloors) {
                                    existingData.setLength(0);
                                    existingData.append("Existing Floors and Slots:\n");
                                    for (String sortedFloor : floorNames) {
                                        Integer count = floorSlotCounts.get(sortedFloor);
                                        if (count != null) {
                                            existingData.append(sortedFloor)
                                                    .append(": ")
                                                    .append(count)
                                                    .append(" slots\n");
                                        }
                                    }
                                    tvExistingData.setText(existingData.toString());
                                    progressBar.setVisibility(View.GONE); // Hide progress bar
                                }
                            })
                            .addOnFailureListener(e -> {
                                completed[0]++;
                                if (completed[0] == totalFloors) {
                                    tvExistingData.setText(existingData.toString());
                                    progressBar.setVisibility(View.GONE); // Hide progress bar
                                }
                            });
                }
            } else {
                tvExistingData.setText("No existing parking data found.");
                progressBar.setVisibility(View.GONE); // Hide progress bar
            }
        }).addOnFailureListener(e -> {
            tvExistingData.setText("Failed to load existing data.");
            progressBar.setVisibility(View.GONE); // Hide progress bar
        });
    }

    private int extractFloorNumber(String floorName) {
        try {
            return Integer.parseInt(floorName.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return 0; // Handle non-numeric floor names
        }
    }

    private void generateFloorInputs() {
        String floorsStr = etNumFloors.getText().toString().trim();
        if (TextUtils.isEmpty(floorsStr)) {
            Toast.makeText(getContext(), "Please enter number of floors", Toast.LENGTH_SHORT).show();
            return;
        }

        numFloors = Integer.parseInt(floorsStr);
        floorContainer.removeAllViews();

        // Generate floor inputs in order
        for (int i = 1; i <= numFloors; i++) {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView textView = new TextView(getContext());
            textView.setText("Floor " + i + ": ");
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1
            ));

            EditText editText = new EditText(getContext());
            editText.setHint("Enter slots");
            editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            editText.setId(i);
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    2
            ));

            layout.addView(textView);
            layout.addView(editText);
            floorContainer.addView(layout);
        }

        btnSave.setVisibility(View.VISIBLE);
    }

    private void saveParkingDetails() {
        if (numFloors == 0) {
            Toast.makeText(getContext(), "No floors to save", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference buildingDocRef = db.collection("building").document(ownerLocation);

        // Create floors in numerical order
        for (int i = 1; i <= numFloors; i++) {
            EditText slotInput = floorContainer.findViewById(i);
            if (slotInput == null) continue;

            String slotsStr = slotInput.getText().toString().trim();
            if (TextUtils.isEmpty(slotsStr)) {
                Toast.makeText(getContext(), "Please enter slots for all floors", Toast.LENGTH_SHORT).show();
                return;
            }

            int numSlots = Integer.parseInt(slotsStr);
            DocumentReference floorDoc = buildingDocRef.collection("floors").document("floor" + i);
            batch.set(floorDoc, new HashMap<>());

            // Create slots in order
            for (int j = 1; j <= numSlots; j++) {
                DocumentReference slotDoc = floorDoc.collection("slots").document("slot" + j);
                Map<String, Object> slotData = new HashMap<>();
                slotData.put("status", "available");
                slotData.put("userphone", "Not Assigned");
                slotData.put("useremail", "Not Assigned");
                slotData.put("username", "Not Assigned");
                slotData.put("carno", "Not Assigned");
                slotData.put("timefrom", "");
                slotData.put("timeto", "");
                slotData.put("amount", "50");

                batch.set(slotDoc, slotData);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Parking details saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}