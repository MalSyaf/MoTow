package com.example.motow.admin;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.motow.LoginActivity;
import com.example.motow.R;
import com.example.motow.databinding.ActivityAdminBinding;
import com.example.motow.users.Users;
import com.example.motow.users.UsersAdapter;
import com.example.motow.vehicles.Vehicle;
import com.example.motow.vehicles.VehicleAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityAdminBinding binding;

    // Firebase
    private FirebaseFirestore fStore;
    private String userId;

    private ArrayList<Users> users;
    private UsersAdapter usersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getUid();

        getUsers();
        setUpRecyclerView();
        setListeners();
    }

    private void getUsers() {
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").whereEqualTo("isVerified", null)
                .get()
                .addOnCompleteListener(task -> {
                    String currentUserId = fAuth.getUid();
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Users> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            Users users1 = new Users();
                            users1.pfp = queryDocumentSnapshot.getString("image");
                            users1.name = queryDocumentSnapshot.getString("name");
                            users.add(users1);
                        }
                        if (users.size() > 0) {
                            UsersAdapter usersAdapter = new UsersAdapter(users);
                            binding.verifyUserRecycler.setAdapter(usersAdapter);
                            binding.noUserText.setVisibility(View.GONE);
                        }
                    }
                });

        db.collection("Users").whereNotEqualTo("isVerified", null)
                .get()
                .addOnCompleteListener(task -> {
                    String currentUserId = fAuth.getUid();
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Users> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            Users users1 = new Users();
                            users1.pfp = queryDocumentSnapshot.getString("image");
                            users1.name = queryDocumentSnapshot.getString("name");
                            users.add(users1);
                        }
                        if (users.size() > 0) {
                            UsersAdapter usersAdapter = new UsersAdapter(users);
                            binding.usersRecycler.setAdapter(usersAdapter);
                            binding.emptyUsers.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setUpRecyclerView() {
        binding.verifyUserRecycler.setHasFixedSize(true);
        binding.verifyUserRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.usersRecycler.setHasFixedSize(true);
        binding.usersRecycler.setLayoutManager(new LinearLayoutManager(this));
        users = new ArrayList<>();
        usersAdapter = new UsersAdapter(users);
        binding.verifyUserRecycler.setAdapter(usersAdapter);
        binding.usersRecycler.setAdapter(usersAdapter);
    }

    private void setListeners() {
        binding.imageMenu.setOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navigtationView.setNavigationItemSelectedListener(this);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void eventChangeListener() {
        fStore.collection("Vehicles").whereEqualTo("ownerId", userId)
                .addSnapshotListener((value, error) -> {
                    assert value != null;
                    if (value.isEmpty()) {
                        binding.noUserText.setVisibility(View.VISIBLE);
                    }
                    if (error != null) {
                        return;
                    }
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            users.add(dc.getDocument().toObject(Users.class));
                            binding.noUserText.setVisibility(View.GONE);
                        }
                    }
                    usersAdapter.notifyDataSetChanged();
                });
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuUsers:
                break;
            case R.id.menuVehicles:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer,
                        new VehiclesFragment()).commit();
                break;
            case R.id.menuProcesses:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer,
                        new ProcessesFragment()).commit();
                break;
            case R.id.menuLogout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                finish();
                break;
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}