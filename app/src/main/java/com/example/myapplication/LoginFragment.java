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

import com.example.myapplication.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth; // Import

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private NavController navController;
    private FirebaseAuth mAuth; // Declare

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance(); // Initialize

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        binding.ivBackButton.setOnClickListener(v -> {
            if (!navController.popBackStack()) {
                requireActivity().finish();
            }
        });

        binding.tvGoToRegister.setOnClickListener(v -> {
            navController.navigate(R.id.action_loginFragment_to_registerStep1Fragment);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        // --- Login Button ---
        binding.btnLogin.setOnClickListener(v -> {
            if (validateLogin()) {
                String email = binding.etEmail.getText().toString().trim();
                String password = binding.etPassword.getText().toString().trim();

                // --- Firebase Sign In Logic ---
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(requireActivity(), task -> {
                            if (task.isSuccessful()) {
                                // Sign in success
                                Log.d("AUTH", "signInWithEmail:success");
                                Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                                navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
                            } else {
                                // If sign in fails
                                Log.w("AUTH", "signInWithEmail:failure", task.getException());
                                Toast.makeText(getContext(), "Login Failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    // ... (validateLogin method remains the same) ...
    private boolean validateLogin() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return false;
        }
        return true;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

//Used Gemini AI for Genarations and Error Handlings
