package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color; // <-- IMPORT
import android.graphics.drawable.ColorDrawable; // <-- IMPORT
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // <-- IMPORT
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddMealSearchFragment extends Fragment {

    private static final String TAG = "AddMealSearchFragment";
    private static final int REQUEST_SCAN = 2001;

    private RecyclerView rvFoodsList;
    private FoodAdapter foodAdapter;
    private List<Food> allFoods;
    private EditText etSearchFoods;
    private ImageView ivScanBarcode;

    private AddMealViewModel addMealViewModel;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Set<String> likedFoodIds = new HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = requireActivity();
        addMealViewModel = new ViewModelProvider(activity).get(AddMealViewModel.class);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_meal_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFoodsList = view.findViewById(R.id.rvFoodsList);
        etSearchFoods = view.findViewById(R.id.etSearchFoods);
        ivScanBarcode = view.findViewById(R.id.ivScanBarcode);

        ivScanBarcode.setOnClickListener(v -> {
            // Open scanner activity
            Intent intent = new Intent(requireContext(), ScanBarcodeActivity.class);
            startActivityForResult(intent, REQUEST_SCAN);
        });

        loadUserLikesAndThenFoods();

        etSearchFoods.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (foodAdapter != null) {
                    foodAdapter.filter(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearchFoods.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearchFoods, InputMethodManager.SHOW_IMPLICIT);
    }

    private void loadUserLikesAndThenFoods() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> likedList = (List<String>) documentSnapshot.get("likedFoods");
                            if (likedList != null) {
                                likedFoodIds.addAll(likedList);
                                Log.d(TAG, "Loaded " + likedFoodIds.size() + " liked food IDs.");
                            }
                        }
                        loadFoodsFromFirestore();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error loading user likes, loading foods anyway.", e);
                        loadFoodsFromFirestore();
                    });
        } else {
            loadFoodsFromFirestore();
        }
    }

    private void loadFoodsFromFirestore() {
        allFoods = new ArrayList<>();

        db.collection("foods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allFoods.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Food food = document.toObject(Food.class);
                        if (food == null) continue;
                        food.setDocumentId(document.getId());
                        allFoods.add(food);
                    }

                    // sort: liked first, then alphabetical
                    Collections.sort(allFoods, (food1, food2) -> {
                        boolean isLiked1 = likedFoodIds.contains(food1.getDocumentId());
                        boolean isLiked2 = likedFoodIds.contains(food2.getDocumentId());

                        if (isLiked1 && !isLiked2) return -1;
                        if (!isLiked1 && isLiked2) return 1;
                        return safeName(food1).compareToIgnoreCase(safeName(food2));
                    });

                    Log.d(TAG, "Loaded and sorted " + allFoods.size() + " foods.");

                    // --- SETUP ADAPTER ---
                    if (foodAdapter == null) {
                        foodAdapter = new FoodAdapter(allFoods, food -> showAddGramsDialog(food));
                        rvFoodsList.setAdapter(foodAdapter);
                    } else {
                        foodAdapter.updateData(allFoods);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading foods", e);
                    Toast.makeText(getContext(), "Error loading food data.", Toast.LENGTH_SHORT).show();
                });
    }

    private String safeName(Food f) {
        return f.getName() != null ? f.getName() : "";
    }

    // ------------------------------------------------------------------------
    // Dialog to add grams (keeps your behavior; small optimization)
    // ------------------------------------------------------------------------
    private void showAddGramsDialog(Food food) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_grams, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvFoodName = dialogView.findViewById(R.id.tvFoodNameDialog);
        TextInputEditText etGrams = dialogView.findViewById(R.id.etGrams);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView tvCalculatedCalories = dialogView.findViewById(R.id.tvCalculatedCalories);

        View nutritionRows = dialogView.findViewById(R.id.nutritionRows);
        TextView tvDialogCarbs = nutritionRows.findViewById(R.id.tvDetailCarbs);
        TextView tvDialogProtein = nutritionRows.findViewById(R.id.tvDetailProtein);
        TextView tvDialogFat = nutritionRows.findViewById(R.id.tvDetailFat);

        tvFoodName.setText(food.getName());

        // efficient live calc: avoid creating object each char
        etGrams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int grams = Integer.parseInt(s.toString());
                    if (grams <= 0) throw new NumberFormatException();
                    double multiplier = grams / 100.0;

                    double calories = food.getCalories() * multiplier;
                    double carbs = food.getPer100g() != null ? food.getPer100g().getCarbs() * multiplier : 0.0;
                    double protein = food.getPer100g() != null ? food.getPer100g().getProtein() * multiplier : 0.0;
                    double fat = food.getPer100g() != null ? food.getPer100g().getFat() * multiplier : 0.0;

                    tvCalculatedCalories.setText(String.format(Locale.US, "%d kcal", (int) Math.round(calories)));
                    tvDialogCarbs.setText(String.format(Locale.US, "%.1fg", carbs));
                    tvDialogProtein.setText(String.format(Locale.US, "%.1fg", protein));
                    tvDialogFat.setText(String.format(Locale.US, "%.1fg", fat));

                } catch (NumberFormatException e) {
                    tvCalculatedCalories.setText("0 kcal");
                    tvDialogCarbs.setText("0.0g");
                    tvDialogProtein.setText("0.0g");
                    tvDialogFat.setText("0.0g");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etGrams.setText("100");

        btnAdd.setOnClickListener(v -> {
            String gramsStr = etGrams.getText() != null ? etGrams.getText().toString() : "";
            if (gramsStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a valid gram amount", Toast.LENGTH_SHORT).show();
                return;
            }
            int grams = Integer.parseInt(gramsStr);
            if (grams <= 0) {
                Toast.makeText(getContext(), "Please enter a valid gram amount", Toast.LENGTH_SHORT).show();
                return;
            }
            addMealViewModel.addFoodToMeal(food, grams);
            Toast.makeText(getContext(), "Added " + grams + "g of " + food.getName(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ------------------------------------------------------------------------
    // Handle scan result
    // ------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCAN && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                String barcode = data.getStringExtra("barcode");
                if (barcode != null && !barcode.isEmpty()) {
                    fetchFoodFromAPI(barcode);
                } else {
                    Toast.makeText(requireContext(), "No barcode detected.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // OpenFoodFacts query (network on background thread)
    // ------------------------------------------------------------------------
    private void fetchFoodFromAPI(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v2/product/" + barcode + ".json";
        Log.d(TAG, "Fetching product for barcode: " + barcode + " | URL: " + url);

        new Thread(() -> {
            java.net.HttpURLConnection conn = null;
            try {
                java.net.URL api = new java.net.URL(url);
                conn = (java.net.HttpURLConnection) api.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                int code = conn.getResponseCode();
                Log.d(TAG, "HTTP response code: " + code);

                if (code != 200) {
                    // read error stream
                    java.io.InputStream errorStream = conn.getErrorStream();
                    String errorMsg = "";
                    if (errorStream != null) {
                        java.util.Scanner s = new java.util.Scanner(errorStream).useDelimiter("\\A");
                        errorMsg = s.hasNext() ? s.next() : "";
                    }
                    Log.w(TAG, "Failed to fetch product: " + code + " | " + errorMsg);
                    throw new Exception("Response code " + code);
                }

                java.io.InputStream input = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(input).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                Log.d(TAG, "API response length: " + response.length());

                org.json.JSONObject obj = new org.json.JSONObject(response);
                if (!obj.has("product")) {
                    throw new Exception("No product found in response");
                }

                org.json.JSONObject p = obj.getJSONObject("product");
                Food food = new Food();
                food.setName(p.optString("product_name", "Unknown product"));
                food.setImageUrl(p.optString("image_small_url", p.optString("image_url", "")));
                food.setDescription(p.optString("brands", ""));
                food.setRating(0.0);

                Food.Per100g per = new Food.Per100g();
                org.json.JSONObject n = p.optJSONObject("nutriments");
                if (n == null) n = new org.json.JSONObject();
                per.setCarbs(n.optDouble("carbohydrates_100g", 0));
                per.setProtein(n.optDouble("proteins_100g", 0));
                per.setFat(n.optDouble("fat_100g", 0));
                per.setFibers(n.optDouble("fiber_100g", 0));
                food.setPer100g(per);

                double kcal = n.optDouble("energy-kcal_100g", Double.NaN);
                if (Double.isNaN(kcal) || kcal == 0.0) {
                    double kj = n.optDouble("energy_100g", Double.NaN);
                    kcal = (!Double.isNaN(kj) && kj > 0) ? kj / 4.184 : 0;
                }
                food.setCalories(kcal);

                requireActivity().runOnUiThread(() -> {
                    if (allFoods == null) allFoods = new ArrayList<>();
                    allFoods.add(0, food);

                    if (foodAdapter == null) {
                        foodAdapter = new FoodAdapter(allFoods, f -> showAddGramsDialog(f));
                        rvFoodsList.setAdapter(foodAdapter);
                    } else {
                        foodAdapter.updateData(allFoods);
                    }

                    rvFoodsList.scrollToPosition(0);
                    etSearchFoods.setText("");
                    Toast.makeText(requireContext(), "Scanned and added: " + food.getName(), Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch product: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Product Not Found In API", Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }


}

//Used Gemini AI for Genarations and Error Handlings
