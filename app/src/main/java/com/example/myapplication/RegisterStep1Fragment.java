package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.myapplication.databinding.FragmentRegisterStep1Binding;
import com.google.firebase.auth.FirebaseAuth; // Import
import com.google.firebase.auth.FirebaseAuthUserCollisionException; // Import

public class RegisterStep1Fragment extends Fragment {

    private FragmentRegisterStep1Binding binding;
    private NavController navController;
    private FirebaseAuth mAuth; // Declare Firebase Auth

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterStep1Binding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        binding.ivBackButton.setOnClickListener(v -> navController.popBackStack());

        binding.tvGoToLogin.setOnClickListener(v -> {
            navController.navigate(R.id.action_registerStep1Fragment_to_loginFragment);
        });

        // Continue to Step 2
        binding.btnContinue.setOnClickListener(v -> {
            if (validateStep1()) {
                String email = binding.etEmail.getText().toString().trim();
                String password = binding.etPassword.getText().toString().trim();

                // --- Firebase create user logic ---
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(requireActivity(), task -> {
                            if (task.isSuccessful()) {
                                // Sign up success, user is automatically logged in.
                                Log.d("AUTH", "createUserWithEmail:success");
                                // Navigate to Step 2 to add profile info
                                navController.navigate(R.id.action_registerStep1Fragment_to_registerStep2Fragment);
                            } else {
                                // If sign up fails, display a message.
                                Log.w("AUTH", "createUserWithEmail:failure", task.getException());

                                // Check for specific errors
                                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                    binding.tilEmail.setError("This email address is already in use.");
                                    binding.etEmail.requestFocus();
                                } else {
                                    Toast.makeText(getContext(), "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    /**
     * Validates Email, Password, and Re-entered Password fields.
     * @return true if all fields are valid, false otherwise.
     */
    private boolean validateStep1() {
        // Clear previous errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilRePassword.setError(null);

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String rePassword = binding.etRePassword.getText().toString().trim();

        // Email validation
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        } else if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return false;
        }

        // Re-enter Password validation
        if (TextUtils.isEmpty(rePassword)) {
            binding.tilRePassword.setError("Please re-enter your password");
            binding.etRePassword.requestFocus();
            return false;
        } else if (!password.equals(rePassword)) {
            binding.tilRePassword.setError("Passwords do not match");
            binding.etRePassword.requestFocus();
            return false;
        }

        return true; // All fields are valid
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

//Used Gemini AI for Genarations and Error Handlings
