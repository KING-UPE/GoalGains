package com.example.myapplication;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

// --- 1. ADD THIS IMPORT ---
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    private MealsViewModel mealsViewModel;
    private NavController navController;
    private int currentDestinationId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- 2. ADD THIS CALL *BEFORE* super.onCreate() ---
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mealsViewModel = new ViewModelProvider(this).get(MealsViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
        NavigationUI.setupWithNavController(bottomNavView, navController);

        // ... (Rest of your MainActivity code remains exactly the same) ...

        FloatingActionButton fabAddMeal = findViewById(R.id.fab_add_meal);
        BottomAppBar bottomAppBar = findViewById(R.id.bottom_app_bar);

        // --- FAB CLICK LOGIC ---
        fabAddMeal.setOnClickListener(view -> {
            if (currentDestinationId == R.id.navigation_meals) {
                navigateToPAddMealFromMealsTab();
            } else {
                showMealTypeSelectionDialog();
            }
        });

        // --- Show/Hide FAB and BottomAppBar ---
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            this.currentDestinationId = destination.getId();

            if (destination.getId() == R.id.navigation_add_meal ||
                    destination.getId() == R.id.navigation_goal_setup_auto ||
                    destination.getId() == R.id.navigation_goal_setup_manual ||
                    destination.getId() == R.id.navigation_login ||
                    destination.getId() == R.id.navigation_register_step1 ||
                    destination.getId() == R.id.navigation_register_step2 ||
                    destination.getId() == R.id.navigation_profile ||
                    destination.getId() == R.id.navigation_auth_choice) {
                fabAddMeal.setVisibility(View.GONE);
                bottomAppBar.setVisibility(View.GONE);
            } else {
                fabAddMeal.setVisibility(View.VISIBLE);
                bottomAppBar.setVisibility(View.VISIBLE);
            }
        });

        // --- OLD METHODS / PLACEHOLDER SETUP ---
        setupOldMethods();
    }

    // ... (All your other methods: navigateToPAddMealFromMealsTab, etc.) ...

    /** Navigate from Meals tab using ViewModel's selected date and meal type */
    private void navigateToPAddMealFromMealsTab() {
        LocalDate date = mealsViewModel.selectedDate.getValue();
        String mealType = mealsViewModel.selectedMealType.getValue();

        if (date == null) date = LocalDate.now();
        if (mealType == null) mealType = "Breakfast";

        String dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        navigateToPAddMeal(dateString, mealType);
    }

    /** Show a dialog to select meal type on other tabs */
    private void showMealTypeSelectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_meal, null);

        // This is your working code
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Transparent outer window (This solves the "box issue")
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        String dateString = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        Button btnBreakfast = dialogView.findViewById(R.id.btnDialogBreakfast);
        Button btnLunch = dialogView.findViewById(R.id.btnDialogLunch);
        Button btnDinner = dialogView.findViewById(R.id.btnDialogDinner);
        Button btnSnacks = dialogView.findViewById(R.id.btnDialogSnacks);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnBreakfast.setOnClickListener(v -> {
            navigateToPAddMeal(dateString, "Breakfast");
            dialog.dismiss();
        });
        btnLunch.setOnClickListener(v -> {
            navigateToPAddMeal(dateString, "Lunch");
            dialog.dismiss();
        });
        btnDinner.setOnClickListener(v -> {
            navigateToPAddMeal(dateString, "Dinner");
            dialog.dismiss();
        });
        btnSnacks.setOnClickListener(v -> {
            navigateToPAddMeal(dateString, "Snacks");
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /** Navigate to AddMealFragment with date and mealType */
    private void navigateToPAddMeal(String dateString, String mealType) {
        Bundle args = new Bundle();
        args.putString("selectedDate", dateString);
        args.putString("selectedMealType", mealType);
        navController.navigate(R.id.navigation_add_meal, args);
    }

    /** OLD METHODS PLACEHOLDER */
    private void setupOldMethods() {
        // Keep all old listeners, observers, and other logic
    }

    /** Placeholder for calorie calculation (if needed elsewhere) */
    private int calculateCalories(String foodName, int grams) {
        // This is likely unused now, but safe to keep as a private helper
        return grams;
    }
}

//Used Gemini AI for Genarations and Error Handlings
