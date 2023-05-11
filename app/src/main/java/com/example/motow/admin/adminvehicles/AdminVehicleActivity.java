package com.example.motow.admin.adminvehicles;

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
import com.example.motow.admin.AdminActivity;
import com.example.motow.admin.adminprocesses.AdminProcessActivity;
import com.example.motow.databinding.ActivityAdminVehicleBinding;
import com.example.motow.users.Users;
import com.example.motow.users.UsersAdapter;
import com.example.motow.vehicles.Vehicle;
import com.example.motow.vehicles.VehicleListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AdminVehicleActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, VehicleListener {

    private ActivityAdminVehicleBinding binding;

    private ArrayList<Vehicle> vehicles;
    private AdminVehicleAdapter vehicleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminVehicleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setUpRecyclerView();
        getVehicles();
        setListeners();
    }

    private void setListeners() {
        binding.imageMenu.setOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navigtationView.setNavigationItemSelectedListener(this);
    }

    private void getVehicles() {
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Vehicles")
                .get()
                .addOnCompleteListener(task -> {
                    String currentUserId = fAuth.getUid();
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Vehicle> vehicles = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            Vehicle vehicles1 = new Vehicle();
                            vehicles1.vehicleId = queryDocumentSnapshot.getId();
                            vehicles1.plateNumber = queryDocumentSnapshot.getString("plateNumber");
                            vehicles.add(vehicles1);
                        }
                        if (vehicles.size() > 0) {
                            AdminVehicleAdapter adminVehicleAdapter = new AdminVehicleAdapter(vehicles, this);
                            binding.vehicleRecycler.setAdapter(adminVehicleAdapter);
                            binding.emptyVechicle.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setUpRecyclerView() {
        binding.vehicleRecycler.setHasFixedSize(true);
        binding.vehicleRecycler.setLayoutManager(new LinearLayoutManager(this));
        vehicles = new ArrayList<>();
        vehicleAdapter = new AdminVehicleAdapter(vehicles, this);
        binding.vehicleRecycler.setAdapter(vehicleAdapter);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuUsers:
                startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                break;
            case R.id.menuVehicles:
                break;
            case R.id.menuProcesses:
                startActivity(new Intent(getApplicationContext(), AdminProcessActivity.class));
                break;
            case R.id.menuLogout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onVehicleClicked(Vehicle vehicle) {
        Intent intent = new Intent(getApplicationContext(), AdminVehicleDetailsActivity.class);
        intent.putExtra("vehicleId", vehicle);
        startActivity(intent);
        finish();
    }
}