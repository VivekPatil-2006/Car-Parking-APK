package com.example.admincarparking;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SlotActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SlotAdapter adapter;
    private FirebaseFirestore db;
    private String floorName, ownerLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slot);

        // Get intent extras
        floorName = getIntent().getStringExtra("FLOOR_NAME");
        ownerLocation = getIntent().getStringExtra("LOCATION");

        // Initialize views
        recyclerView = findViewById(R.id.slotsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        fetchSlots();
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SlotAdapter(new ArrayList<>(), this, floorName, ownerLocation);
        recyclerView.setAdapter(adapter);
    }

    private void fetchSlots() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("building").document(ownerLocation)
                .collection("floors").document(floorName)
                .collection("slots")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        List<Slot> slots = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Slot slot = new Slot();
                            slot.setId(doc.getId());
                            slot.setStatus(doc.getString("status"));
                            slots.add(slot);
                        }
                        adapter.updateSlots(slots);
                    } else {
                        Toast.makeText(this, "Error loading slots!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}