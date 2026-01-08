package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication.databinding.FragmentRegisterStep2Binding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RegisterStep2Fragment extends Fragment {

    private FragmentRegisterStep2Binding binding;
    private NavController navController;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterStep2Binding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        setupGenderDropdown();
        setupDatePicker();

        // === FIX 1: Back Button now signs out to cancel registration ===
        binding.ivBackButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                mAuth.signOut();
            }
            navController.popBackStack();
        });

        // === FIX 2: Skip Button now creates a minimal profile ===
        binding.btnSkip.setOnClickListener(v -> {
            saveMinimalProfile();
        });

        binding.btnFinish.setOnClickListener(v -> {
            if (validateStep2()) {
                saveUserProfile();
            }
        });
    }

    /**
     * Helper function to save a minimal profile when user skips.
     * This prevents the dashboard from crashing.
     */
    private void saveMinimalProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            // === THIS LINE IS NOW FIXED ===
            // Navigate back to RegisterStep1 if something is wrong
            navController.popBackStack();
            return;
        }

        String userId = currentUser.getUid();
        DocumentReference userDocRef = db.collection("users").document(userId);

        Map<String, Object> minimalProfile = new HashMap<>();
        minimalProfile.put("email", currentUser.getEmail());
        minimalProfile.put("firstName", "User"); // Default first name
        minimalProfile.put("lastName", "");     // Default last name

        // Add default goals so the dashboard doesn't crash
        minimalProfile.put("dailyCalorieGoal", 0);
        minimalProfile.put("dailyProteinGoal", 0);
        minimalProfile.put("dailyCarbGoal", 0);
        minimalProfile.put("dailyFatGoal", 0);
        minimalProfile.put("targetWeightKg", 0.0);
        // Note: No height, weight, gender, or birthday is saved

        // (You should show a progress bar here)
        userDocRef.set(minimalProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DB", "Minimal user profile successfully written!");
                    // (Hide progress bar)
                    navController.navigate(R.id.action_registerStep2Fragment_to_dashboardFragment);
                })
                .addOnFailureListener(e -> {
                    Log.w("DB", "Error writing minimal profile", e);
                    // (Hide progress bar)
                    Toast.makeText(getContext(), "Error: Could not skip. Please try again.", Toast.LENGTH_SHORT).show();
                    // Stay on this fragment
                });
    }

    private void saveUserProfile() {
        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String birthday = binding.etBirthday.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString().trim();
        String weightStr = binding.etWeight.getText().toString().trim();
        String gender = binding.actvGender.getText().toString().trim();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        DocumentReference userDocRef = db.collection("users").document(userId);
        DocumentReference weightLogRef = userDocRef.collection("weight_logs").document();

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("email", currentUser.getEmail());
        userProfile.put("firstName", firstName);
        userProfile.put("lastName", lastName);

        if (!birthday.isEmpty()) userProfile.put("birthday", birthday);
        if (!gender.isEmpty()) userProfile.put("gender", gender);
        if (!heightStr.isEmpty()) {
            try {
                userProfile.put("heightCm", Double.parseDouble(heightStr));
            } catch (NumberFormatException e) {
                Log.e("DB", "Invalid height format", e);
                binding.tilHeight.setError("Invalid number");
                return; // Stop execution
            }
        }

        userProfile.put("dailyCalorieGoal", 0);
        userProfile.put("dailyProteinGoal", 0);
        userProfile.put("dailyCarbGoal", 0);
        userProfile.put("dailyFatGoal", 0);
        userProfile.put("targetWeightKg", 0.0);

        WriteBatch batch = db.batch();

        if (!weightStr.isEmpty()) {
            try {
                double startingWeight = Double.parseDouble(weightStr);
                userProfile.put("startingWeightKg", startingWeight);

                Map<String, Object> latestWeight = new HashMap<>();
                latestWeight.put("weightKg", startingWeight);
                latestWeight.put("date", Timestamp.now());
                userProfile.put("latestWeight", latestWeight);

                Map<String, Object> weightLogEntry = new HashMap<>();
                weightLogEntry.put("date", Timestamp.now());
                weightLogEntry.put("weightKg", startingWeight);
                batch.set(weightLogRef, weightLogEntry);
            } catch (NumberFormatException e) {
                Log.e("DB", "Invalid weight format", e);
                binding.tilWeight.setError("Invalid number");
                return; // Stop execution
            }
        }

        batch.set(userDocRef, userProfile);

        // (You should show a progress bar here)

        // === FIX 3: Failure listener NO LONGER navigates to dashboard ===
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("DB", "User profile & initial weight successfully written!");
                    // (Hide progress bar)
                    Toast.makeText(getContext(), "Profile saved! Logging in...", Toast.LENGTH_SHORT).show();
                    navController.navigate(R.id.action_registerStep2Fragment_to_dashboardFragment);
                })
                .addOnFailureListener(e -> {
                    Log.w("DB", "Error writing batch", e);
                    // (Hide progress bar)
                    Toast.makeText(getContext(), "Error saving profile. Please try again.", Toast.LENGTH_SHORT).show();
                    // DO NOT NAVIGATE. Stay on this page.
                });
    }

    private void setupGenderDropdown() {
        String[] genders = new String[]{"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                genders
        );
        binding.actvGender.setAdapter(adapter);
    }

    private void setupDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select your birthday")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            binding.etBirthday.setText(sdf.format(selection));
            binding.tilBirthday.setError(null);
        });

        View.OnClickListener showPicker = v -> {
            if (!datePicker.isAdded()) {
                datePicker.show(getParentFragmentManager(), "DATE_PICKER");
            }
        };
        binding.etBirthday.setOnClickListener(showPicker);
        binding.tilBirthday.setEndIconOnClickListener(showPicker);
    }

    private boolean validateStep2() {
        binding.tilFirstName.setError(null);
        binding.tilLastName.setError(null);
        binding.tilHeight.setError(null);
        binding.tilWeight.setError(null);

        String firstName = binding.etFirstName.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString().trim();
        String weightStr = binding.etWeight.getText().toString().trim();
        boolean isValid = true;

        if (TextUtils.isEmpty(firstName)) {
            binding.tilFirstName.setError("First name is required");
            if (isValid) binding.etFirstName.requestFocus();
            isValid = false;
        }
        // Last name is optional

        if (!heightStr.isEmpty()) {
            try { Double.parseDouble(heightStr); } catch (NumberFormatException e) {
                binding.tilHeight.setError("Enter a valid number");
                if (isValid) binding.etHeight.requestFocus();
                isValid = false;
            }
        }

        if (!weightStr.isEmpty()) {
            try { Double.parseDouble(weightStr); } catch (NumberFormatException e) {
                binding.tilWeight.setError("Enter a valid number");
                if (isValid) binding.etWeight.requestFocus();
                isValid = false;
            }
        }
        return isValid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

//Used Gemini AI for Genarations and Error Handlings
