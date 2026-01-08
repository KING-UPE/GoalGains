package com.example.myapplication;

// (All your imports are unchanged)
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.ByteArrayOutputStream;
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
import com.bumptech.glide.Glide;
import com.example.myapplication.databinding.FragmentProfileBinding;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private NavController navController;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;
    private FirebaseUser currentUser;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            Glide.with(requireContext())
                                    .load(uri)
                                    .circleCrop()
                                    .into(binding.ivProfilePic);
                            convertAndSaveImage(uri);
                        }
                    }
                }
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            setupGenderDropdown();
            setupDatePicker();
            loadUserData();
            setEditMode(false); // Set initial state to read-only
        } else {
            navController.navigate(R.id.action_global_logout);
        }

        binding.ivBackButton.setOnClickListener(v -> navController.popBackStack());

        binding.ivEditButton.setOnClickListener(v -> setEditMode(true));

        binding.btnCancel.setOnClickListener(v -> {
            setEditMode(false);
            loadUserData(); // Reload data to discard any changes
        });

        binding.btnSave.setOnClickListener(v -> {
            saveUserData();
        });

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            navController.navigate(R.id.action_global_logout);
            Toast.makeText(getContext(), "Logged Out", Toast.LENGTH_SHORT).show();
        });

        binding.ivProfilePic.setOnClickListener(v -> handleChangePhoto());
        binding.tvChangePhoto.setOnClickListener(v -> handleChangePhoto());
    }

    private void setEditMode(boolean isEditing) {
        binding.ivEditButton.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        binding.editModeButtons.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        binding.tvChangePhoto.setVisibility(isEditing ? View.VISIBLE : View.GONE);

        // --- THIS IS THE MODIFIED LINE ---
        // Hide/Show the logout button based on edit mode
        binding.btnLogout.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        // --- END OF MODIFICATION ---

        binding.ivProfilePic.setClickable(isEditing);

        binding.etFirstName.setEnabled(isEditing);
        binding.etLastName.setEnabled(isEditing);
        binding.etBirthday.setEnabled(isEditing);
        binding.etHeight.setEnabled(isEditing);
        binding.etTargetWeight.setEnabled(isEditing); // This is your "Current Weight" field
        binding.actvGender.setEnabled(isEditing);
    }

    private void handleChangePhoto() {
        if (binding.etFirstName.isEnabled()) {
            pickImageLauncher.launch("image/*");
        } else {
            Toast.makeText(getContext(), "Tap the edit icon to change your photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserData() {
        if (currentUserId == null) return;

        DocumentReference userDocRef = db.collection("users").document(currentUserId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d("Profile", "Document data: " + document.getData());

                    String base64Image = document.getString("profilePictureBase64");
                    if (base64Image != null && !base64Image.isEmpty() && getContext() != null) {
                        try {
                            byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            Glide.with(requireContext())
                                    .load(bitmap)
                                    .circleCrop()
                                    .into(binding.ivProfilePic);
                        } catch (Exception e) {
                            Log.e("ProfileLoad", "Error decoding Base64 image", e);
                            binding.ivProfilePic.setImageResource(R.drawable.ic_person);
                        }
                    } else {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_person);
                    }

                    binding.etEmail.setText(document.getString("email"));
                    binding.etFirstName.setText(document.getString("firstName"));
                    binding.etLastName.setText(document.getString("lastName"));
                    binding.etBirthday.setText(document.getString("birthday"));
                    binding.actvGender.setText(document.getString("gender"), false);

                    if (document.contains("heightCm")) {
                        binding.etHeight.setText(String.format(Locale.US, "%.0f", document.getDouble("heightCm")));
                    }
                    if (document.contains("dailyCalorieGoal")) {
                        binding.etCalorieGoal.setText(String.valueOf(document.getLong("dailyCalorieGoal")));
                    }

                    if (document.contains("latestWeight")) {
                        Map<String, Object> latestWeight = (Map<String, Object>) document.get("latestWeight");
                        if (latestWeight != null && latestWeight.containsKey("weightKg")) {
                            binding.etTargetWeight.setText(String.format(Locale.US, "%.1f", (Double) latestWeight.get("weightKg")));
                        }
                    } else if (document.contains("targetWeightKg")) {
                        binding.etTargetWeight.setText(String.format(Locale.US, "%.1f", document.getDouble("targetWeightKg")));
                    }

                } else {
                    Log.d("Profile", "No such document");
                }
            } else {
                Log.d("Profile", "get failed with ", task.getException());
            }
        });
    }

    private void saveUserData() {
        if (currentUserId == null) return;

        binding.tilFirstName.setError(null);
        binding.tilHeight.setError(null);
        binding.tilWeight.setError(null);

        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String birthday = binding.etBirthday.getText().toString().trim();
        String gender = binding.actvGender.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString().trim();
        String currentWeightStr = binding.etTargetWeight.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(firstName)) {
            binding.tilFirstName.setError("First name is required");
            isValid = false;
        }

        double height = 0;
        if (!heightStr.isEmpty()) {
            try { height = Double.parseDouble(heightStr); } catch (NumberFormatException e) {
                binding.tilHeight.setError("Invalid number");
                isValid = false;
            }
        }

        double currentWeight = 0;
        if (!currentWeightStr.isEmpty()) {
            try { currentWeight = Double.parseDouble(currentWeightStr); } catch (NumberFormatException e) {
                binding.tilWeight.setError("Invalid number");
                isValid = false;
            }
        }

        if (!isValid) {
            Toast.makeText(getContext(), "Please fix the errors", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("birthday", birthday);
        userData.put("gender", gender);
        if (height > 0) userData.put("heightCm", height);

        if (currentWeight > 0) {
            Map<String, Object> latestWeight = new HashMap<>();
            latestWeight.put("weightKg", currentWeight);
            latestWeight.put("date", Timestamp.now());

            userData.put("latestWeight", latestWeight);
        }

        db.collection("users").document(currentUserId)
                .update(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
                    setEditMode(false);
                })
                .addOnFailureListener(e -> {
                    Log.w("Profile", "Error updating document", e);
                    Toast.makeText(getContext(), "Error saving profile.", Toast.LENGTH_SHORT).show();
                });
    }

    // (All other methods: convertAndSaveImage, saveBase64ImageToFirestore,
    // setupGenderDropdown, setupDatePicker, and onDestroyView are unchanged)

    private void convertAndSaveImage(Uri imageUri) {
        if (currentUserId == null || getContext() == null) return;
        Toast.makeText(getContext(), "Processing photo...", Toast.LENGTH_SHORT).show();
        try {
            Bitmap originalBitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                originalBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContext().getContentResolver(), imageUri));
            } else {
                originalBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            saveBase64ImageToFirestore(base64Image);
        } catch (IOException e) {
            Log.e("ProfileUpload", "Error converting image", e);
            Toast.makeText(getContext(), "Error processing photo.", Toast.LENGTH_SHORT).show();
            loadUserData();
        }
    }

    private void saveBase64ImageToFirestore(String base64Image) {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId)
                .update("profilePictureBase64", base64Image)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w("ProfileUpload", "Error saving Base64 to Firestore", e);
                    Toast.makeText(getContext(), "Error saving photo.", Toast.LENGTH_SHORT).show();
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
            if (binding.etFirstName.isEnabled()) {
                if (!datePicker.isAdded()) {
                    datePicker.show(getParentFragmentManager(), "DATE_PICKER");
                }
            }
        };
        binding.etBirthday.setOnClickListener(showPicker);
        binding.tilBirthday.setEndIconOnClickListener(showPicker);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

//Used Gemini AI for Genarations and Error Handlings
