package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddMealFragment extends Fragment {

    private static final String TAG = "AddMealFragment";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialCardView bottomCartControls;
    private NavController navController;

    private AddMealViewModel addMealViewModel;

    private TextView tvCartTotalCalories;
    private TextView tvCartTotalCarbs;
    private TextView tvCartTotalProtein;
    private TextView tvCartTotalFat;
    private Button btnSaveMeal;
    private Button btnAddToLog;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String selectedDate;
    private String selectedMealType;
    private boolean isSaving = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selectedDate = getArguments().getString("selectedDate");
            selectedMealType = getArguments().getString("selectedMealType");
        } else {
            selectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            selectedMealType = "Breakfast";
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        tabLayout = view.findViewById(R.id.tabLayoutAddMeal);
        viewPager = view.findViewById(R.id.viewPagerAddMeal);
        bottomCartControls = view.findViewById(R.id.bottomCartControls);

        ImageView ivBackButton = view.findViewById(R.id.ivBackButton);
        ivBackButton.setOnClickListener(v -> navController.popBackStack());

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText("Add to " + selectedMealType);

        tvCartTotalCalories = view.findViewById(R.id.tvCartTotalCalories);
        tvCartTotalCarbs = view.findViewById(R.id.tvCartTotalCarbs);
        tvCartTotalProtein = view.findViewById(R.id.tvCartTotalProtein);
        tvCartTotalFat = view.findViewById(R.id.tvCartTotalFat);
        btnSaveMeal = view.findViewById(R.id.btnSaveMeal);
        btnAddToLog = view.findViewById(R.id.btnAddToLog);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FragmentActivity activity = requireActivity();
        addMealViewModel = new ViewModelProvider(activity).get(AddMealViewModel.class);

        AddMealTabAdapter adapter = new AddMealTabAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Search"); break;
                case 1: tab.setText("My Meals"); break;
                case 2: tab.setText("Create"); break;
            }
        }).attach();

        setupObservers();

        btnSaveMeal.setOnClickListener(v -> showSaveMealDialog());
        btnAddToLog.setOnClickListener(v -> addToLog());
    }

    private void setupObservers() {
        addMealViewModel.currentMealEntries.observe(getViewLifecycleOwner(), entries -> {
            bottomCartControls.setVisibility(entries == null || entries.isEmpty() ? View.GONE : View.VISIBLE);
        });
        addMealViewModel.totalCalories.observe(getViewLifecycleOwner(),
                calories -> tvCartTotalCalories.setText(calories + " kcal"));
        addMealViewModel.totalCarbs.observe(getViewLifecycleOwner(),
                carbs -> tvCartTotalCarbs.setText(addMealViewModel.formatDouble(carbs)));
        addMealViewModel.totalProtein.observe(getViewLifecycleOwner(),
                protein -> tvCartTotalProtein.setText(addMealViewModel.formatDouble(protein)));
        addMealViewModel.totalFat.observe(getViewLifecycleOwner(),
                fat -> tvCartTotalFat.setText(addMealViewModel.formatDouble(fat)));
    }

    private void showSaveMealDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to save a meal", Toast.LENGTH_SHORT).show();
            return;
        }
        List<FoodEntry> entries = addMealViewModel.currentMealEntries.getValue();
        if (entries == null || entries.isEmpty()) {
            Toast.makeText(getContext(), "Add food to the cart first", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_save_meal, null);
        TextInputEditText etMealName = dialogView.findViewById(R.id.etMealName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String mealName = etMealName.getText() != null
                    ? etMealName.getText().toString().trim()
                    : "";
            if (mealName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            saveMealToFirestore(currentUser.getUid(), mealName, entries);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void saveMealToFirestore(String uid, String mealName, List<FoodEntry> entries) {
        Map<String, Object> mealData = new HashMap<>();
        mealData.put("name", mealName);
        mealData.put("createdAt", FieldValue.serverTimestamp());
        mealData.put("foodEntries", entries);

        db.collection("users").document(uid)
                .collection("saved_meals")
                .add(mealData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "'" + mealName + "' saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error saving meal.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Error saving meal", e);
                });
    }

    private void addToLog() {
        if (isSaving) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to add a meal.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        List<FoodEntry> entries = addMealViewModel.currentMealEntries.getValue();

        if (entries == null || entries.isEmpty()) {
            Toast.makeText(getContext(), "Your cart is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer totalCalories = addMealViewModel.totalCalories.getValue();
        Double totalCarbs = addMealViewModel.totalCarbs.getValue();
        Double totalProtein = addMealViewModel.totalProtein.getValue();
        Double totalFat = addMealViewModel.totalFat.getValue();

        isSaving = true;
        btnAddToLog.setEnabled(false);

        DocumentReference dateDocRef = db.collection("users")
                .document(uid)
                .collection("daily_logs")
                .document(selectedDate);

        WriteBatch batch = db.batch();

        for (FoodEntry entry : entries) {
            LoggedFoodItem loggedItem = new LoggedFoodItem(entry.getFood(), entry.getGrams());
            DocumentReference newFoodRef = dateDocRef.collection(selectedMealType).document();
            batch.set(newFoodRef, loggedItem);
        }

        Map<String, Object> dailyTotals = new HashMap<>();
        dailyTotals.put("totalCalories", FieldValue.increment(totalCalories));
        dailyTotals.put("totalCarbs", FieldValue.increment(totalCarbs));
        dailyTotals.put("totalProtein", FieldValue.increment(totalProtein));
        dailyTotals.put("totalFat", FieldValue.increment(totalFat));

        batch.set(dateDocRef, dailyTotals, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Added to " + selectedMealType, Toast.LENGTH_SHORT).show();
                        addMealViewModel.clearCurrentMeal();
                        navController.popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) {
                        Log.w(TAG, "Error writing batch", e);
                        Toast.makeText(getContext(), "Error saving meal.", Toast.LENGTH_SHORT).show();
                        btnAddToLog.setEnabled(true);
                        isSaving = false;
                    }
                });
    }
}


//Used Gemini AI for Genarations and Error Handlings
