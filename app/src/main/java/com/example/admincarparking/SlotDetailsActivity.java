package com.example.admincarparking;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SlotDetailsActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserPhone, tvCarNo, tvTimeFrom, tvTimeTo, tvAmount;
    private Button btnCancelBooking, btnAcceptBooking;
    private FirebaseFirestore db;
    private String floorName, ownerLocation, slotId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slot_details);

        // Initialize views
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvCarNo = findViewById(R.id.tvCarNo);
        tvTimeFrom = findViewById(R.id.tvTimeFrom);
        tvTimeTo = findViewById(R.id.tvTimeTo);
        tvAmount = findViewById(R.id.tvAmount);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);
        //btnAcceptBooking = findViewById(R.id.btnAcceptBooking);

        // Get intent extras
        floorName = getIntent().getStringExtra("FLOOR_NAME");
        ownerLocation = getIntent().getStringExtra("LOCATION");
        slotId = getIntent().getStringExtra("SLOT_ID");

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Fetch slot details
        fetchSlotDetails();

        // Set button click listeners
        btnCancelBooking.setOnClickListener(v -> cancelBooking());
        //btnAcceptBooking.setOnClickListener(v -> acceptBooking());
    }

    private void fetchSlotDetails() {
        db.collection("building").document(ownerLocation)
                .collection("floors").document(floorName)
                .collection("slots").document(slotId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate user details
                        tvUserName.setText("Name: " + documentSnapshot.getString("username"));
                        tvUserEmail.setText("Email: " + documentSnapshot.getString("useremail"));
                        tvUserPhone.setText("Phone: " + documentSnapshot.getString("userphone"));
                        tvCarNo.setText("Car No: " + documentSnapshot.getString("carno"));
                        tvTimeFrom.setText("From: " + documentSnapshot.getString("timefrom"));
                        tvTimeTo.setText("To: " + documentSnapshot.getString("timeto"));
                        tvAmount.setText("Amount: " + documentSnapshot.getString("amount"));
                    } else {
                        Toast.makeText(this, "Slot details not found!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching slot details!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void cancelBooking() {
        // Update slot status to "available" and clear user details
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "available");
        updates.put("username", "Not Assigned");
        updates.put("useremail", "Not Assigned");
        updates.put("userphone", "Not Assigned");
        updates.put("carno", "");
        updates.put("timefrom", "");
        updates.put("timeto", "");

        db.collection("building").document(ownerLocation)
                .collection("floors").document(floorName)
                .collection("slots").document(slotId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking canceled successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel booking!", Toast.LENGTH_SHORT).show();
                });
    }

    private void acceptBooking() {
        // Update slot status to "booked"
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "booked");

        db.collection("building").document(ownerLocation)
                .collection("floors").document(floorName)
                .collection("slots").document(slotId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking accepted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to accept booking!", Toast.LENGTH_SHORT).show();
                });
    }
}