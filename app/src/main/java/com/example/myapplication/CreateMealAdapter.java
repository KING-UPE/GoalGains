package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the "Create" (cart) list (uses list_item_meal_food.xml)
 */
public class    CreateMealAdapter extends RecyclerView.Adapter<CreateMealAdapter.CreateMealViewHolder> {

    private List<FoodEntry> foodEntries;
    private final OnRemoveItemClickListener listener;

    /**
     * Interface for the remove button.
     * The CreateMealFragment implements this.
     */
    public interface OnRemoveItemClickListener {
        void onRemoveItemClick(FoodEntry foodEntry);
    }

    public CreateMealAdapter(List<FoodEntry> foodEntries, OnRemoveItemClickListener listener) {
        this.foodEntries = foodEntries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CreateMealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_meal_food, parent, false);
        return new CreateMealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CreateMealViewHolder holder, int position) {
        FoodEntry entry = foodEntries.get(position);
        holder.bind(entry, listener);
    }

    @Override
    public int getItemCount() {
        return foodEntries.size();
    }

    /**
     * Updates the list when the ViewModel's LiveData changes.
     */
    public void updateList(List<FoodEntry> newList) {
        this.foodEntries = newList;
        notifyDataSetChanged(); // Note: For real apps, use DiffUtil for efficiency
    }

    // --- ViewHolder Class ---
    static class CreateMealViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoodImage, ivRemoveItem;
        TextView tvFoodName, tvFoodWeight;

        public CreateMealViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage = itemView.findViewById(R.id.ivFoodImage);
            ivRemoveItem = itemView.findViewById(R.id.ivRemoveItem);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodWeight = itemView.findViewById(R.id.tvFoodWeight);
        }

        public void bind(final FoodEntry entry, final OnRemoveItemClickListener listener) {
            tvFoodName.setText(entry.getFood().getName());
            tvFoodWeight.setText(String.format(Locale.US, "%dg", entry.getGrams()));

            // --- Use Glide to load the image URL ---
            String imageUrl = entry.getFood().getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_food_placeholder)
                        .error(R.drawable.ic_food_placeholder)
                        .circleCrop() // Matches your layout
                        .into(ivFoodImage);
            } else {
                // Set default placeholder if no URL
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_food_placeholder)
                        .circleCrop()
                        .into(ivFoodImage);
            }
            // --- END OF FIX ---

            // Set the listener for the 'x' button
            ivRemoveItem.setOnClickListener(v -> listener.onRemoveItemClick(entry));
        }
    }
}

//Used Gemini AI for Genarations and Error Handlings
