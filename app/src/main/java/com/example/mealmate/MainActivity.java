package com.example.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.*;

public class MainActivity extends AppCompatActivity {

    private TextView textGreeting;
    private TextView textMealStats;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textGreeting = findViewById(R.id.textGreeting);
        textMealStats = findViewById(R.id.textMealStats);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            String greeting = getGreetingBasedOnTime();
            textGreeting.setText(greeting + "!");
        }

        //Initialize Firestore
        db = FirebaseFirestore.getInstance();

        //load meal stats
        loadMealStats();

        findViewById(R.id.menuMealRecipes).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MealRecipeActivity.class)));

        findViewById(R.id.menuMealPlanner).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MealPlannerActivity.class)));

        findViewById(R.id.menuItemDelegation).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ItemDelegationActivity.class)));

        findViewById(R.id.menuLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private String getGreetingBasedOnTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        else if (hour < 17) return "Good afternoon";
        else return "Good evening";
    }

    private void loadMealStats() {
        db.collection("users")
                .document(userId)
                .collection("recipes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int planned = 0;
                    int completed = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        boolean tracked = Boolean.TRUE.equals(doc.getBoolean("tracked"));
                        if (!tracked) continue;

                        planned++;

                        Map<String, Boolean> purchasedMap = (Map<String, Boolean>) doc.get("purchasedMap");
                        List<String> ingredients = (List<String>) doc.get("ingredients");

                        boolean isCompleted = true;
                        if (ingredients != null) {
                            for (String ing : ingredients) {
                                if (purchasedMap == null || !Boolean.TRUE.equals(purchasedMap.get(ing))) {
                                    isCompleted = false;
                                    break;
                                }
                            }
                        }

                        if (isCompleted) completed++;
                    }

                    int left = planned - completed;

                    List<String> messages = new ArrayList<>();
                    messages.add("You have " + completed + " meals completed this week.");
                    messages.add("You have " + planned + " meals planned this week.");
                    messages.add("You have " + left + " meals left to complete this week.");

                    Random rand = new Random();
                    String chosenMessage = messages.get(rand.nextInt(messages.size()));
                    textMealStats.setText(chosenMessage);
                });
    }
}
