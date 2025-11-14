package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ItemDelegationActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;

    private RecyclerView recyclerView;
    private Button buttonSendSMS;
    private List<Recipe> trackedMeals = new ArrayList<>();
    private ShareMealsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_delegation);

        //Toolbar
        Toolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        //drawer setup
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this::handleNavigation);

        //Views
        recyclerView = findViewById(R.id.recyclerViewShareMeals);
        buttonSendSMS = findViewById(R.id.buttonSendSMS);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShareMealsAdapter(trackedMeals);
        recyclerView.setAdapter(adapter);

        //load tracked meals
        loadTrackedMeals();

        //Send SMS button
        buttonSendSMS.setOnClickListener(v -> sendShoppingList());
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_meal_recipes) {
            startActivity(new Intent(this, MealRecipeActivity.class));
        } else if (id == R.id.nav_meal_planner) {
            startActivity(new Intent(this, MealPlannerActivity.class));
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        return true;
    }

    private void loadTrackedMeals() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("recipes")
                .whereEqualTo("tracked", true)
                .get()
                .addOnSuccessListener(snapshot -> {
                    trackedMeals.clear();
                    for (var doc : snapshot) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        List<String> ingredients = (List<String>) doc.get("ingredients");
                        String steps = doc.getString("steps");
                        boolean tracked = Boolean.TRUE.equals(doc.getBoolean("tracked"));

                        Recipe recipe = new Recipe(id, name, ingredients, steps, tracked);
                        trackedMeals.add(recipe);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load meals", Toast.LENGTH_SHORT).show());
    }

    private void sendShoppingList() {
        List<Recipe> selectedMeals = adapter.getSelectedMeals();

        if (selectedMeals.isEmpty()) {
            Toast.makeText(this, "Select at least one meal", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build message
        StringBuilder message = new StringBuilder("Shopping List:\n\n");
        for (Recipe recipe : selectedMeals) {
            message.append("• ").append(recipe.getName()).append("\n");
            for (String ing : recipe.getIngredients()) {
                message.append("   - ").append(ing).append("\n");
            }
            message.append("\n");
        }

        // Launch SMS intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse("sms:")); // Open default SMS app
        intent.putExtra("sms_body", message.toString());
        startActivity(intent);
    }
}
