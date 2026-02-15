package com.example.admincarparking;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.ViewHolder> {

    private List<Slot> slots;
    private Context context;
    private String floorName, ownerLocation;

    public SlotAdapter(List<Slot> slots, Context context, String floorName, String ownerLocation) {
        this.slots = slots;
        this.context = context;
        this.floorName = floorName;
        this.ownerLocation = ownerLocation;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Slot slot = slots.get(position);
        holder.slotNumber.setText(slot.getId());

        // Set background color based on slot status
        int bgColor = slot.getStatus().equalsIgnoreCase("available") ?
                Color.parseColor("#4CAF50") : // Green for available
                Color.parseColor("#F44336");  // Red for booked
        holder.slotNumber.setBackgroundColor(bgColor);

        // Set click listener for booked slots
        if (slot.getStatus().equalsIgnoreCase("booked")) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, SlotDetailsActivity.class);
                intent.putExtra("FLOOR_NAME", floorName);
                intent.putExtra("LOCATION", ownerLocation);
                intent.putExtra("SLOT_ID", slot.getId());
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public void updateSlots(List<Slot> newSlots) {
        slots = newSlots;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView slotNumber;

        ViewHolder(View itemView) {
            super(itemView);
            slotNumber = itemView.findViewById(R.id.slotNumber);
        }
    }
}