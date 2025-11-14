package com.example.mealmate;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class AddRecipeActivity extends AppCompatActivity {

    private String recipeId;
    private boolean isEditMode = false;

    private TextInputEditText inputName, inputIngredients, inputSteps;
    private SwitchMaterial switchTrack;
    private Button buttonSave, buttonCancel;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        inputName = findViewById(R.id.inputName);
        inputIngredients = findViewById(R.id.inputIngredients);
        inputSteps = findViewById(R.id.inputSteps);
        switchTrack = findViewById(R.id.switchTrack);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);

        // edit mode
        if ("edit".equals(getIntent().getStringExtra("mode"))) {
            isEditMode = true;
            recipeId = getIntent().getStringExtra("id");

            inputName.setText(getIntent().getStringExtra("name"));
            inputSteps.setText(getIntent().getStringExtra("steps"));
            switchTrack.setChecked(getIntent().getBooleanExtra("tracked", false));

            List<String> ingredientsList = getIntent().getStringArrayListExtra("ingredients");
            if (ingredientsList != null) {
                inputIngredients.setText(String.join("\n", ingredientsList));
            }
        }

        // Cancel
        buttonCancel.setOnClickListener(v -> finish());

        // Save button
        buttonSave.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String ingredientsRaw = inputIngredients.getText().toString().trim();
            String steps = inputSteps.getText().toString().trim();
            boolean tracked = switchTrack.isChecked();

            if (name.isEmpty()) {
                inputName.setError("Recipe name is required");
                inputName.requestFocus();
                return;
            }

            List<String> ingredients = Arrays.asList(ingredientsRaw.split("\\n"));
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> recipeMap = new HashMap<>();
            recipeMap.put("name", name);
            recipeMap.put("ingredients", ingredients);
            recipeMap.put("steps", steps);
            recipeMap.put("tracked", tracked);

            if (isEditMode) {
                db.collection("users")
                        .document(userId)
                        .collection("recipes")
                        .document(recipeId)
                        .set(recipeMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Recipe updated successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                db.collection("users")
                        .document(userId)
                        .collection("recipes")
                        .add(recipeMap)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Recipe added successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}
