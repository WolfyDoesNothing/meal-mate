package com.example.mealmate;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MealPlannerActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private List<Recipe> trackedRecipes = new ArrayList<>();
    private MealPlannerAdapter adapter;

    private final Set<String> expandedIds = new HashSet<>();

    //shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastAccel;
    private float currentAccel;
    private float accel;
    private static final float SHAKE_THRESHOLD = 12f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);

        Toolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(this::handleNavigation);

        recyclerView = findViewById(R.id.recyclerViewMealPlanner);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MealPlannerAdapter(this, trackedRecipes, expandedIds);
        recyclerView.setAdapter(adapter);

        loadTrackedMeals();

        //setup shake
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        lastAccel = SensorManager.GRAVITY_EARTH;
        currentAccel = SensorManager.GRAVITY_EARTH;
        accel = 0.00f;
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_meal_recipes) {
            startActivity(new Intent(this, MealRecipeActivity.class));
        } else if (id == R.id.nav_delegation) {
            startActivity(new Intent(this, ItemDelegationActivity.class));
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
                    trackedRecipes.clear();
                    for (var doc : snapshot) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        List<String> ingredients = (List<String>) doc.get("ingredients");
                        String steps = doc.getString("steps");
                        boolean tracked = Boolean.TRUE.equals(doc.getBoolean("tracked"));

                        Recipe recipe = new Recipe(id, name, ingredients, steps, tracked);

                        Object rawMap = doc.get("purchasedMap");
                        if (rawMap instanceof Map) {
                            Map<String, Boolean> map = new HashMap<>();
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawMap).entrySet()) {
                                if (entry.getKey() instanceof String && entry.getValue() instanceof Boolean) {
                                    map.put((String) entry.getKey(), (Boolean) entry.getValue());
                                }
                            }
                            recipe.setPurchasedMap(map);
                        }

                        trackedRecipes.add(recipe);
                        expandedIds.add(id);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading meals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    //shake
    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            lastAccel = currentAccel;
            currentAccel = (float) Math.sqrt((x * x + y * y + z * z));
            float delta = currentAccel - lastAccel;
            accel = accel * 0.9f + delta;

            if (accel > SHAKE_THRESHOLD) {
                promptClearPurchased();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private void promptClearPurchased() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Purchased Items")
                .setMessage("Shake detected!\nDo you want to reset all ingredient checkmarks?")
                .setPositiveButton("Yes", (dialog, which) -> clearAllPurchasedIngredients())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllPurchasedIngredients() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Recipe recipe : trackedRecipes) {
            Map<String, Boolean> clearedMap = new HashMap<>();
            for (String ingredient : recipe.getIngredients()) {
                clearedMap.put(ingredient, false);
            }
            recipe.setPurchasedMap(clearedMap);
            db.collection("users")
                    .document(userId)
                    .collection("recipes")
                    .document(recipe.getId())
                    .update("purchasedMap", clearedMap);
        }

        adapter.notifyDataSetChanged();
        Toast.makeText(this, "All ingredient checkmarks cleared.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrackedMeals();

        //sensor
        if (accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }
}
