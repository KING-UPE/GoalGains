package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class AuthChoiceFragment extends Fragment {

    private NavController navController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_choice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        Button btnLogin = view.findViewById(R.id.btnLoginChoice);
        Button btnRegister = view.findViewById(R.id.btnRegisterChoice);
        Button btnCancel = view.findViewById(R.id.btnCancelChoice);

        btnLogin.setOnClickListener(v ->
                navController.navigate(R.id.action_authChoiceFragment_to_loginFragment));

        btnRegister.setOnClickListener(v ->
                navController.navigate(R.id.action_authChoiceFragment_to_registerStep1Fragment));

        btnCancel.setOnClickListener(v -> navController.popBackStack()); // Go back to Dashboard
    }
}
//Used Gemini AI for Genarations and Error Handlings
