package com.example.myapplication;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class FoodDetailFragment extends Fragment {

    private static final String TAG = "FoodDetailFragment";
    private Window window;

    private Food foodItem;
    private boolean isLiked = false;

    private NavController navController;
    private ImageView ivFavorite;
    private TextView tvDescription;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userDocRef;

    public FoodDetailFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (getArguments() != null) {
            foodItem = (Food) getArguments().getSerializable("food_item");
            String docId = getArguments().getString("food_id");
            if (foodItem != null && docId != null) {
                foodItem.setDocumentId(docId);
            }
        }

        if (currentUser != null) {
            userDocRef = db.collection("users").document(currentUser.getUid());
        }

        if (getActivity() != null) {
            window = getActivity().getWindow();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_food_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        ImageView ivBackButton = view.findViewById(R.id.ivBackButton);
        ivBackButton.setOnClickListener(v -> navController.popBackStack());

        ivFavorite = view.findViewById(R.id.ivFavorite);
        tvDescription = view.findViewById(R.id.tvDescription);

        if (foodItem == null) return;

        populateFoodData(view);
        loadLikeStatus();

        ivFavorite.setOnClickListener(v -> handleLikeClick());
    }

    private void populateFoodData(View view) {
        ImageView ivFoodImageBig = view.findViewById(R.id.ivFoodImageBig);
        TextView tvFoodNameDetail = view.findViewById(R.id.tvFoodNameDetail);
        TextView tvFoodCategoryDetail = view.findViewById(R.id.tvFoodCategoryDetail);
        TextView tvRating = view.findViewById(R.id.tvRating);
        TextView tvCarbsPer100g = view.findViewById(R.id.tvCarbsPer100g);
        TextView tvProteinPer100g = view.findViewById(R.id.tvProteinPer100g);
        TextView tvFatPer100g = view.findViewById(R.id.tvFatPer100g);

        tvFoodNameDetail.setText(foodItem.getName());
        tvFoodCategoryDetail.setText(foodItem.getMainCategory());
        tvRating.setText(String.format(Locale.US, "%.1f", foodItem.getRating()));

        if (foodItem.getDescription() != null && !foodItem.getDescription().isEmpty()) {
            tvDescription.setText(foodItem.getDescription());
        }

        if (foodItem.getImageUrl() != null && !foodItem.getImageUrl().isEmpty()) {
            Glide.with(this).load(foodItem.getImageUrl()).into(ivFoodImageBig);
        }

        // Use helper methods (Firestore-safe)
        tvCarbsPer100g.setText(String.format(Locale.US, "%.1fg", foodItem.carbsPer100()));
        tvProteinPer100g.setText(String.format(Locale.US, "%.1fg", foodItem.proteinPer100()));
        tvFatPer100g.setText(String.format(Locale.US, "%.1fg", foodItem.fatPer100()));

        double netCarbs = foodItem.carbsPer100() - foodItem.fibersPer100();
        if (netCarbs < 0) netCarbs = 0;
        double carbCalories = netCarbs * 4;
        double proteinCalories = foodItem.proteinPer100() * 4;
        double fatCalories = foodItem.fatPer100() * 9;
        double totalCalories = carbCalories + proteinCalories + fatCalories;

        int carbsPercent = (totalCalories > 0) ? (int) ((carbCalories / totalCalories) * 100) : 0;
        int proteinPercent = (totalCalories > 0) ? (int) ((proteinCalories / totalCalories) * 100) : 0;
        int fatPercent = (totalCalories > 0) ? (int) ((fatCalories / totalCalories) * 100) : 0;

        CircularProgressIndicator carbsRing = view.findViewById(R.id.progressCarbsRing);
        CircularProgressIndicator proteinRing = view.findViewById(R.id.progressProteinRing);
        CircularProgressIndicator fatRing = view.findViewById(R.id.progressFatRing);

        TextView tvCarbsPercent = view.findViewById(R.id.tvCarbsPercent);
        TextView tvProteinPercent = view.findViewById(R.id.tvProteinPercent);
        TextView tvFatPercent = view.findViewById(R.id.tvFatPercent);

        animateRingWithText(carbsRing, tvCarbsPercent, carbsPercent);
        animateRingWithText(proteinRing, tvProteinPercent, proteinPercent);
        animateRingWithText(fatRing, tvFatPercent, fatPercent);
    }

    private void loadLikeStatus() {
        updateHeartIcon(false);
        if (currentUser != null && userDocRef != null && foodItem != null && foodItem.getDocumentId() != null) {
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> likedFoods = (List<String>) documentSnapshot.get("likedFoods");
                    if (likedFoods != null && likedFoods.contains(foodItem.getDocumentId())) {
                        isLiked = true;
                        updateHeartIcon(true);
                    }
                }
            }).addOnFailureListener(e -> Log.w(TAG, "Error loading user data", e));
        }
    }

    private void handleLikeClick() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to like foods", Toast.LENGTH_SHORT).show();
            navController.navigate(R.id.action_global_authChoiceFragment);
            return;
        }

        String foodId = foodItem.getDocumentId();
        if (foodId == null || foodId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Food ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLiked) {
            userDocRef.update("likedFoods", FieldValue.arrayRemove(foodId))
                    .addOnSuccessListener(aVoid -> {
                        isLiked = false;
                        updateHeartIcon(false);
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Error unliking food", e));
        } else {
            userDocRef.update("likedFoods", FieldValue.arrayUnion(foodId))
                    .addOnSuccessListener(aVoid -> {
                        isLiked = true;
                        updateHeartIcon(true);
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Error liking food", e));
        }
    }

    private void updateHeartIcon(boolean liked) {
        if (getContext() == null) return;
        int color = liked ? R.color.accent_pink : R.color.text_secondary;
        ivFavorite.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), color)));
    }

    private void animateRingWithText(CircularProgressIndicator ring, TextView textView, int targetProgress) {
        ObjectAnimator ringAnimator = ObjectAnimator.ofInt(ring, "progress", 0, targetProgress);
        ringAnimator.setDuration(1000);
        ringAnimator.start();

        ValueAnimator textAnimator = ValueAnimator.ofInt(0, targetProgress);
        textAnimator.setDuration(1000);
        textAnimator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue() + "%"));
        textAnimator.start();
    }

    private boolean isUsingLightTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (window != null) {
            window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusBarColor));
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowCompat.getInsetsController(window, window.getDecorView())
                    .setAppearanceLightStatusBars(isUsingLightTheme());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (window != null) {
            window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.statusBarColor));
            WindowCompat.setDecorFitsSystemWindows(window, true);
            WindowCompat.getInsetsController(window, window.getDecorView())
                    .setAppearanceLightStatusBars(isUsingLightTheme());
        }
    }
}


//Used Gemini AI for Genarations and Error Handlings
