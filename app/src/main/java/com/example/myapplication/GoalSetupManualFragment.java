package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class GoalSetupManualFragment extends Fragment {

    private NavController navController;
    private ProgressViewModel progressViewModel;

    private TextInputEditText etCalories, etCarbs, etProtein, etFat;
    private Button btnSaveManualGoal;
    private TextView tvCalculation, tvCalculationMatch;

    private int goalCalories = 0;
    private int goalCarbs = 0;
    private int goalProtein = 0;
    private int goalFat = 0;

    // Flag to prevent infinite loop when setting etCalories text
    private boolean isUpdatingCalories = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goal_setup_manual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        // This is the correct scope to share the ViewModel
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);

        findViews(view);

        // **Disable etCalories to make it non-editable (read-only)**
        etCalories.setFocusable(false);
        etCalories.setKeyListener(null); // Prevents opening the keyboard

        ImageView ivBackButton = view.findViewById(R.id.ivBackButton);
        ivBackButton.setOnClickListener(v -> navController.popBackStack());

        setupTextWatchers();

        btnSaveManualGoal.setOnClickListener(v -> saveGoals());

        // Initial calculation on view creation
        updateCalculations();
    }

    private void findViews(View view) {
        etCalories = view.findViewById(R.id.etCalories);
        etCarbs = view.findViewById(R.id.etCarbs);
        etProtein = view.findViewById(R.id.etProtein);
        etFat = view.findViewById(R.id.etFat);
        btnSaveManualGoal = view.findViewById(R.id.btnSaveManualGoal);
        tvCalculation = view.findViewById(R.id.tvCalculation);
        tvCalculationMatch = view.findViewById(R.id.tvCalculationMatch);
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Prevent infinite loop from etCalories.setText() calling this
                if (!isUpdatingCalories) {
                    updateCalculations();
                }
            }
        };

        // We only need to watch Carbs, Protein, and Fat now
        etCarbs.addTextChangedListener(textWatcher);
        etProtein.addTextChangedListener(textWatcher);
        etFat.addTextChangedListener(textWatcher);
    }

    private int getIntFromEditText(TextInputEditText et) {
        try {
            return Integer.parseInt(et.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateCalculations() {
        // Read Carbs, Protein, and Fat only
        goalCarbs = getIntFromEditText(etCarbs);
        goalProtein = getIntFromEditText(etProtein);
        goalFat = getIntFromEditText(etFat);

        // (Carbs * 4) + (Protein * 4) + (Fat * 9)
        // This calculated total is now the main goalCalories
        int newGoalCalories = (goalCarbs * 4) + (goalProtein * 4) + (goalFat * 9);

        // **Update the UI with the calculated calorie goal**
        if (newGoalCalories != goalCalories) {
            goalCalories = newGoalCalories;

            isUpdatingCalories = true; // Set flag before updating text
            if (goalCalories > 0) {
                // Update the calories EditText
                etCalories.setText(String.valueOf(goalCalories));
            } else {
                // Clear the calories EditText if the total is 0
                etCalories.setText("");
            }
            isUpdatingCalories = false; // Reset flag
        }

        String calcText = String.format(Locale.US,
                "(%d * 4) + (%d * 4) + (%d * 9) = %,d kcal",
                goalCarbs, goalProtein, goalFat, goalCalories);
        tvCalculation.setText(calcText);

        // Since goalCalories is always equal to the calculated total,
        // the calculation check is now just a check for completeness (if > 0)
        if (goalCalories > 0) {
            tvCalculationMatch.setText("Calculated from macros.");
            // Use a color that indicates success or validity
            tvCalculationMatch.setTextColor(ContextCompat.getColor(requireContext(), R.color.macro_protein));
            btnSaveManualGoal.setEnabled(true);
        } else {
            tvCalculationMatch.setText("Enter macro values to calculate");
            tvCalculationMatch.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            btnSaveManualGoal.setEnabled(false);
        }
    }

    private void saveGoals() {
        // Now we just need to ensure goalCalories is greater than 0
        if (goalCalories > 0) {
            progressViewModel.saveGoals(goalCalories, goalCarbs, goalProtein, goalFat);
            Toast.makeText(getContext(), "Goal Saved!", Toast.LENGTH_SHORT).show();
            navController.popBackStack(); // Go back to ProgressFragment
        } else {
            Toast.makeText(getContext(), "Please enter valid macro goals.", Toast.LENGTH_SHORT).show();
        }
    }
}


//Used Gemini AI for Genarations and Error Handlings
