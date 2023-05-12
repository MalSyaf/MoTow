package com.example.motow.admin.adminusers;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.motow.LoginActivity;
import com.example.motow.R;
import com.example.motow.admin.AdminActivity;
import com.example.motow.admin.adminprocesses.AdminProcessActivity;
import com.example.motow.admin.adminvehicles.AdminVehicleActivity;
import com.example.motow.databinding.ActivityAdminUserBinding;
import com.example.motow.users.UserListener;
import com.example.motow.users.Users;
import com.example.motow.users.UsersAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminUserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, UserListener {

    private ActivityAdminUserBinding binding;

    private ArrayList<Users> users;
    private UsersAdapter usersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getUsers();
        setUpRecyclerView();
        setListeners();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void getUsers() {
        FirebaseAuth fAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                            users1.userId = queryDocumentSnapshot.getId();
                            users1.pfp = queryDocumentSnapshot.getString("pfp");
                            users1.name = queryDocumentSnapshot.getString("name");
                            users.add(users1);
                        }
                        if (users.size() > 0) {
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.usersRecycler.setAdapter(usersAdapter);
                            binding.emptyUsersText.setVisibility(View.GONE);
                        }
                        usersAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void setUpRecyclerView() {
        binding.usersRecycler.setHasFixedSize(true);
        binding.usersRecycler.setLayoutManager(new LinearLayoutManager(this));
        users = new ArrayList<>();
        usersAdapter = new UsersAdapter(users, this);
        binding.usersRecycler.setAdapter(usersAdapter);
    }

    private void setListeners() {
        binding.imageMenu.setOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navigtationView.setNavigationItemSelectedListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuHome:
                startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                break;
            case R.id.menuUsers:
                break;
            case R.id.menuVehicles:
                startActivity(new Intent(getApplicationContext(), AdminVehicleActivity.class));
                break;
            case R.id.menuProcesses:
                startActivity(new Intent(getApplicationContext(), AdminProcessActivity.class));
                break;
            case R.id.menuDelete:
                startActivity(new Intent(getApplicationContext(), UserDeleteActivity.class));
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
    public void onUserClicked(Users users) {
        Intent intent = new Intent(getApplicationContext(), UserVerificationActivity.class);
        intent.putExtra("userId", users);
        startActivity(intent);
        finish();
    }
}