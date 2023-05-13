package com.example.motow.admin.adminprocesses;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.motow.LoginActivity;
import com.example.motow.R;
import com.example.motow.admin.AdminActivity;
import com.example.motow.admin.adminusers.AdminUserActivity;
import com.example.motow.admin.adminvehicles.AdminVehicleActivity;
import com.example.motow.databinding.ActivityAdminProcessBinding;
import com.example.motow.processes.ProcessListener;
import com.example.motow.processes.Processes;
import com.example.motow.processes.ProcessesAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminProcessActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ProcessListener {

    private ActivityAdminProcessBinding binding;
    private ArrayList<Processes> processes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminProcessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setUpRecyclerView();
        getVehicles();
        setListeners();
    }

    private void setListeners() {
        binding.imageMenu.setOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navigtationView.setNavigationItemSelectedListener(this);

        binding.searchStatus.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchData(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchData(s);
                return false;
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchData(String s) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Processes").whereEqualTo("processId", s)
                .get()
                .addOnCompleteListener(task -> {
                    processes.clear();
                    for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        Processes processes1 = new Processes();
                        processes1.processId = queryDocumentSnapshot.getId();
                        processes.add(processes1);
                    }
                    if (processes.size() > 0) {
                        ProcessesAdapter processesAdapter1 = new ProcessesAdapter(processes, this);
                        binding.statusRecycler.setAdapter(processesAdapter1);
                        binding.emptyStatus.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(e -> {

                });
    }

    private void getVehicles() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Processes")
                .get()
                .addOnCompleteListener(task -> {
                   if(task.isSuccessful() && task.getResult() != null) {
                       List<Processes> processes = new ArrayList<>();
                       for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                           Processes processes1 = new Processes();
                           processes1.processId = queryDocumentSnapshot.getId();
                           processes.add(processes1);
                       }
                       if (processes.size() > 0) {
                           ProcessesAdapter processesAdapter1 = new ProcessesAdapter(processes, this);
                           binding.statusRecycler.setAdapter(processesAdapter1);
                           binding.emptyStatus.setVisibility(View.GONE);
                       }
                   }
                });
    }

    private void setUpRecyclerView() {
        binding.statusRecycler.setHasFixedSize(true);
        binding.statusRecycler.setLayoutManager(new LinearLayoutManager(this));
        processes = new ArrayList<>();
        ProcessesAdapter processesAdapter = new ProcessesAdapter(processes, this);
        binding.statusRecycler.setAdapter(processesAdapter);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuHome:
                startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                break;
            case R.id.menuUsers:
                startActivity(new Intent(getApplicationContext(), AdminUserActivity.class));
                break;
            case R.id.menuVehicles:
                startActivity(new Intent(getApplicationContext(), AdminVehicleActivity.class));
                break;
            case R.id.menuProcesses:
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
    public void onProcessClicked(Processes processes) {
        Intent intent = new Intent(getApplicationContext(), AdminProcessDetails.class);
        intent.putExtra("processId", processes);
        startActivity(intent);
        finish();
    }
}