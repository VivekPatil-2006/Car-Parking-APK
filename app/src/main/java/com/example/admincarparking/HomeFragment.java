package com.example.admincarparking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private LinearLayout parentLayout;
    private ProgressBar progressBar;
    private String ownerLocation;
    private FirebaseFirestore db;
    private List<String> floorList = new ArrayList<>();
    private Map<String, int[]> floorDataMap = new HashMap<>();
    private int completedFetches = 0;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        parentLayout = view.findViewById(R.id.parentLayout);
        progressBar = view.findViewById(R.id.progressBar);

        // Get owner location from SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        ownerLocation = sharedPreferences.getString("ownerLocation", "");

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Fetch floors data
        fetchFloors();

        return view;
    }

    private void fetchFloors() {
        if (ownerLocation.isEmpty()) {
            Toast.makeText(getContext(), "No location found!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        floorList.clear();

        // Fetch floors from Firestore
        db.collection("building").document(ownerLocation).collection("floors")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        parentLayout.removeAllViews();
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(getContext(), "No floors found!", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        // Collect all floor names
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            floorList.add(document.getId());
                        }

                        // Sort floors numerically
                        Collections.sort(floorList, (f1, f2) -> {
                            int num1 = extractFloorNumber(f1);
                            int num2 = extractFloorNumber(f2);
                            return Integer.compare(num1, num2);
                        });

                        // Fetch slot data for all floors
                        fetchSlotDataForAllFloors();
                    } else {
                        Toast.makeText(getContext(), "Error loading floors!", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchSlotDataForAllFloors() {
        floorDataMap.clear();
        completedFetches = 0;

        for (String floor : floorList) {
            db.collection("building").document(ownerLocation)
                    .collection("floors").document(floor).collection("slots")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            int available = 0;
                            int booked = 0;

                            for (QueryDocumentSnapshot slot : task.getResult()) {
                                String status = slot.getString("status");
                                if ("available".equalsIgnoreCase(status)) {
                                    available++;
                                } else {
                                    booked++;
                                }
                            }

                            // Store data with floor name as key
                            floorDataMap.put(floor, new int[]{available, booked});
                        }

                        completedFetches++;

                        // Check if all floor data is collected
                        if (completedFetches == floorList.size()) {
                            // Add cards in sorted order after all data is available
                            updateUIWithSortedFloors();
                        }
                    });
        }
    }

    private void updateUIWithSortedFloors() {
        parentLayout.removeAllViews();

        // Iterate through sorted floor list and create cards
        for (String floor : floorList) {
            int[] counts = floorDataMap.get(floor);
            if (counts != null) {
                addFloorCard(floor, counts[0], counts[1]);
            }
        }

        progressBar.setVisibility(View.GONE);
    }

    private void addFloorCard(String floorName, int available, int booked) {
        // Create CardView
        CardView cardView = new CardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);
        cardView.setLayoutParams(params);
        cardView.setCardElevation(8);
        cardView.setRadius(16);
        cardView.setContentPadding(24, 24, 24, 24);

        // Create LinearLayout for card content
        LinearLayout cardContent = new LinearLayout(requireContext());
        cardContent.setOrientation(LinearLayout.VERTICAL);

        // Create TextViews for floor, available, and booked slots
        TextView tvFloor = createTextView(20, "🏠 " + floorName);
        TextView tvAvailable = createTextView(16, "Available: " + available);
        TextView tvBooked = createTextView(16, "Booked: " + booked);

        // Add TextViews to card content
        cardContent.addView(tvFloor);
        cardContent.addView(tvAvailable);
        cardContent.addView(tvBooked);

        // Add card content to CardView
        cardView.addView(cardContent);

        // Set click listener to navigate to SlotActivity
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SlotActivity.class);
            intent.putExtra("FLOOR_NAME", floorName);
            intent.putExtra("LOCATION", ownerLocation);
            startActivity(intent);
        });

        // Add CardView to parent layout
        parentLayout.addView(cardView);
    }

    private TextView createTextView(int textSize, String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setPadding(0, 0, 0, 8);
        return textView;
    }

    private int extractFloorNumber(String floorName) {
        try {
            return Integer.parseInt(floorName.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return 0; // Handle non-numeric floor names if any
        }
    }
}