package com.example.myapplication;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

// *** NEW IMPORTS ***
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
// *** END NEW IMPORTS ***

import java.util.Locale;
import java.util.Map;

public class GoalSetupAutoFragment extends Fragment {

    private NavController navController;
    private ProgressViewModel progressViewModel;

    // *** FIREBASE VARIABLES ***
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Form Inputs
    private RadioGroup rgGender, rgActivity, rgGoal;
    private TextInputEditText etAge, etWeight, etHeight;
    private Slider sliderIntensity;
    private TextView tvIntensityLabel;
    private Button btnCalculate, btnSaveGoal;

    // Results Views
    private MaterialCardView cardResults;
    private TextView tvResultCalories;
    private View resultCarbsView, resultProteinView, resultFatView;

    // Stored calculated values
    private int finalCalories = 0;
    private int finalCarbs = 0;
    private int finalProtein = 0;
    private int finalFat = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goal_setup_auto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);

        // *** SIMPLIFIED AUTH CHECK ***
        // We can now assume the user is logged in because ProgressFragment checked.
        // We'll just get the user, or pop back if something went wrong.
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Safety check - if user is null (e.g., auth expired), pop back.
        if (currentUser == null) {
            Log.e("GoalSetupAuto", "User is null, popping back to safety.");
            // This pops back to ProgressFragment, which will handle the auth check.
            navController.popBackStack();
            return;
        }
        // *** END OF AUTH CHECK ***

        findViews(view);

        // --- Load user data to pre-fill the form ---
        loadUserData();

        ImageView ivBackButton = view.findViewById(R.id.ivBackButton);
        ivBackButton.setOnClickListener(v -> navController.popBackStack());

        sliderIntensity.addOnChangeListener((slider, value, fromUser) -> {
            int level = (int) value;
            if (level == 1) tvIntensityLabel.setText("Level 1: Gentle (10%)");
            else if (level == 2) tvIntensityLabel.setText("Level 2: Normal (20%)");
            else if (level == 3) tvIntensityLabel.setText("Level 3: Brutal (30%)");
        });

        btnCalculate.setOnClickListener(v -> calculateGoals());
        btnSaveGoal.setOnClickListener(v -> saveGoals());
    }

    private void findViews(View view) {
        rgGender = view.findViewById(R.id.rgGender);
        rgActivity = view.findViewById(R.id.rgActivity);
        rgGoal = view.findViewById(R.id.rgGoal);
        etAge = view.findViewById(R.id.etAge);
        etWeight = view.findViewById(R.id.etWeight);
        etHeight = view.findViewById(R.id.etHeight);
        sliderIntensity = view.findViewById(R.id.sliderIntensity);
        tvIntensityLabel = view.findViewById(R.id.tvIntensityLabel);
        btnCalculate = view.findViewById(R.id.btnCalculate);
        btnSaveGoal = view.findViewById(R.id.btnSaveGoal);
        cardResults = view.findViewById(R.id.cardResults);
        tvResultCalories = view.findViewById(R.id.tvResultCalories);
        resultCarbsView = view.findViewById(R.id.resultCarbs);
        resultProteinView = view.findViewById(R.id.resultProtein);
        resultFatView = view.findViewById(R.id.resultFat);
    }

    /**
     * *** MODIFIED METHOD ***
     * Fetches user data from Firestore and pre-fills the form fields.
     */
    private void loadUserData() {
        // The currentUser null check is no longer needed here,
        // as it's handled in onViewCreated.

        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());
        userDocRef.get().addOnSuccessListener(document -> {
            if (document.exists() && getContext() != null) {
                // Pre-fill Gender
                String gender = document.getString("gender");
                if (gender != null) {
                    if (gender.equalsIgnoreCase("Male")) {
                        rgGender.check(R.id.rbMale);
                    } else if (gender.equalsIgnoreCase("Female")) {
                        rgGender.check(R.id.rbFemale);
                    } else if (gender.equalsIgnoreCase("Other")) {
                        rgGender.check(R.id.rbOther);
                    }
                }

                // Pre-fill Height
                if (document.contains("heightCm")) {
                    double height = document.getDouble("heightCm");
                    etHeight.setText(String.format(Locale.US, "%.0f", height));
                }

                // Pre-fill Weight (try latestWeight first, then startingWeight)
                if (document.contains("latestWeight")) {
                    Map<String, Object> latestWeight = (Map<String, Object>) document.get("latestWeight");
                    if (latestWeight != null && latestWeight.containsKey("weightKg")) {
                        etWeight.setText(String.format(Locale.US, "%.1f", (Double) latestWeight.get("weightKg")));
                    }
                } else if (document.contains("startingWeightKg")) {
                    // Fallback to starting weight
                    double startWeight = document.getDouble("startingWeightKg");
                    etWeight.setText(String.format(Locale.US, "%.1f", startWeight));
                }

                // Pre-fill Age by calculating from birthday
                String birthday = document.getString("birthday");
                if (birthday != null && !birthday.isEmpty()) {
                    int age = calculateAge(birthday);
                    if (age > 0) {
                        etAge.setText(String.valueOf(age));
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("GoalSetupAuto", "Error loading user data", e);
        });
    }

    /**
     * *** (NO CHANGE) ***
     * Calculates age from a "yyyy-MM-dd" birthday string.
     */
    private int calculateAge(String birthdayString) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return 0; // java.time APIs require API 26 (Oreo)
        }
        try {
            LocalDate birthDate = LocalDate.parse(birthdayString);
            LocalDate today = LocalDate.now();
            return Period.between(birthDate, today).getYears();
        } catch (DateTimeParseException e) {
            Log.e("GoalSetupAuto", "Error parsing birthday", e);
            return 0;
        }
    }


    private void calculateGoals() {
        // --- 1. Validate and Get Inputs ---
        if (etAge.getText().toString().isEmpty() ||
                etWeight.getText().toString().isEmpty() ||
                etHeight.getText().toString().isEmpty() ||
                rgGender.getCheckedRadioButtonId() == -1 ||
                rgActivity.getCheckedRadioButtonId() == -1 ||
                rgGoal.getCheckedRadioButtonId() == -1) {

            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight = Double.parseDouble(etWeight.getText().toString());
        double height = Double.parseDouble(etHeight.getText().toString());
        int age = Integer.parseInt(etAge.getText().toString());

        // --- 2. Calculate BMR (Mifflin-St Jeor) ---
        double bmr;
        int checkedGenderId = rgGender.getCheckedRadioButtonId();
        if (checkedGenderId == R.id.rbMale) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else if (checkedGenderId == R.id.rbFemale) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        } else { // "Other" - use average
            double bmrMale = (10 * weight) + (6.25 * height) - (5 * age) + 5;
            double bmrFemale = (10 * weight) + (6.25 * height) - (5 * age) - 161;
            bmr = (bmrMale + bmrFemale) / 2.0;
        }

        // --- 3. Calculate TDEE (Maintenance Calories) ---
        double activityFactor = 1.2; // Default to Sedentary
        int checkedActivityId = rgActivity.getCheckedRadioButtonId();
        if (checkedActivityId == R.id.rbModerate) {
            activityFactor = 1.55;
        } else if (checkedActivityId == R.id.rbActive) {
            activityFactor = 1.7; // Using 1.7 as a sample for "Very Active"
        }
        double tdee = bmr * activityFactor;

        // --- 4. Apply Cut/Bulk Goal ---
        double goalFactor = 0.0;
        int intensity = (int) sliderIntensity.getValue();
        double intensityPercent = (intensity == 1) ? 0.10 : (intensity == 2) ? 0.20 : 0.30;

        int checkedGoalId = rgGoal.getCheckedRadioButtonId();
        if (checkedGoalId == R.id.rbCut) {
            goalFactor = -intensityPercent; // -10%, -20%, or -30%
        } else if (checkedGoalId == R.id.rbBulk) {
            goalFactor = intensityPercent; // +10%, +20%, or +30%
        }
        // if "Maintain", goalFactor remains 0.0

        double calculatedCalories = tdee * (1.0 + goalFactor);

        // --- 5. Calculate Macros ---
        // Protein: 2.0g per kg of body weight
        double proteinGrams = 2.0 * weight;
        double proteinCalories = proteinGrams * 4.0;

        // Fat: 25% of total calories
        double fatCalories = calculatedCalories * 0.25;
        double fatGrams = fatCalories / 9.0;

        // Carbs: Remaining calories
        double carbCalories = calculatedCalories - proteinCalories - fatCalories;
        double carbGrams = carbCalories / 4.0;

        // --- 6. Store and Display Results ---
        finalCalories = (int) calculatedCalories;
        finalCarbs = (int) carbGrams;
        finalProtein = (int) proteinGrams;
        finalFat = (int) fatGrams;


        tvResultCalories.setText(String.format(Locale.US, "%,d kcal", finalCalories));
        updateMacroView(resultCarbsView, "Carbs", finalCarbs, R.color.macro_carbs);
        updateMacroView(resultProteinView, "Protein", finalProtein, R.color.macro_protein);
        updateMacroView(resultFatView, "Fat", finalFat, R.color.macro_fat);

        cardResults.setVisibility(View.VISIBLE);
        btnSaveGoal.setVisibility(View.VISIBLE);
        btnCalculate.setText("Re-Calculate");
    }

    private void saveGoals() {
        if (finalCalories == 0) {
            Toast.makeText(getContext(), "Please calculate goals first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressViewModel.saveGoals(finalCalories, finalCarbs, finalProtein, finalFat);
        Toast.makeText(getContext(), "Goal Saved!", Toast.LENGTH_SHORT).show();
        navController.popBackStack(); // Go back to ProgressFragment
    }

    private void updateMacroView(View macroView, String label, Integer value, int colorResId) {
        if (getContext() == null) return;
        TextView tvLabel = macroView.findViewById(R.id.tvMacroLabel);
        TextView tvValue = macroView.findViewById(R.id.tvMacroValue);
        View macroColorDot = macroView.findViewById(R.id.macroColorDot);

        tvLabel.setText(label);
        tvValue.setText(String.format(Locale.US, "%dg", value));
        macroColorDot.setBackgroundColor(getContext().getColor(colorResId));
    }
}

//Used Gemini AI for Genarations and Error Handlings
