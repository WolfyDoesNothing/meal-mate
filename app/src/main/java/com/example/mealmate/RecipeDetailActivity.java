package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {

    private String recipeId;
    private String recipeName;
    private List<String> ingredients;
    private String steps;
    private boolean tracked;

    private FirebaseFirestore db;
    private String userId;

    private TextView textRecipeName, textIngredients, textSteps;
    private Switch switchTracked;
    private Button buttonEdit, buttonDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        Toolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_meal_planner) {
                startActivity(new Intent(this, MealPlannerActivity.class));
            } else if (id == R.id.nav_delegation) {
                startActivity(new Intent(this, ItemDelegationActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            return true;
        });


        textRecipeName = findViewById(R.id.textRecipeName);
        textIngredients = findViewById(R.id.textIngredients);
        textSteps = findViewById(R.id.textSteps);
        switchTracked = findViewById(R.id.switchTracked);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //get recipe data
        recipeId = getIntent().getStringExtra("id");
        recipeName = getIntent().getStringExtra("name");
        steps = getIntent().getStringExtra("steps");
        tracked = getIntent().getBooleanExtra("tracked", false);
        ingredients = getIntent().getStringArrayListExtra("ingredients");

        textRecipeName.setText(recipeName);
        textSteps.setText(steps);
        switchTracked.setChecked(tracked);

        if (ingredients != null) {
            StringBuilder builder = new StringBuilder();
            for (String item : ingredients) {
                builder.append("• ").append(item).append("\n");
            }
            textIngredients.setText(builder.toString());
        }

        //Track switch
        switchTracked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            db.collection("users")
                    .document(userId)
                    .collection("recipes")
                    .document(recipeId)
                    .update("tracked", isChecked)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Tracking updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating tracking", Toast.LENGTH_SHORT).show());
        });

        //Delete
        buttonDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Recipe")
                    .setMessage("Are you sure you want to delete \"" + recipeName + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        db.collection("users")
                                .document(userId)
                                .collection("recipes")
                                .document(recipeId)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show();
                                    finish(); // Go back to MealRecipeActivity
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


        //Edit
        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRecipeActivity.class);
            intent.putExtra("mode", "edit");
            intent.putExtra("id", recipeId);
            intent.putExtra("name", recipeName);
            intent.putExtra("ingredients", new ArrayList<>(ingredients));
            intent.putExtra("steps", steps);
            intent.putExtra("tracked", switchTracked.isChecked());
            startActivityForResult(intent, 101);
        });


        // TODO: Load data from Intent and display

        String id = getIntent().getStringExtra("id");
        String name = getIntent().getStringExtra("name");
        String steps = getIntent().getStringExtra("steps");
        boolean tracked = getIntent().getBooleanExtra("tracked", false);
        ArrayList<String> ingredients = getIntent().getStringArrayListExtra("ingredients");

        //display
        textRecipeName.setText(name);
        textSteps.setText(steps);
        switchTracked.setChecked(tracked);

        //format ingredients as multiline text
        if (ingredients != null) {
            StringBuilder builder = new StringBuilder();
            for (String item : ingredients) {
                builder.append("• ").append(item).append("\n");
            }
            textIngredients.setText(builder.toString());
        }


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK) {
            finish();
        }
    }

}
