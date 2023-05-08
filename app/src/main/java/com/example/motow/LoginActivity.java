package com.example.motow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.admin.AdminActivity;
import com.example.motow.databinding.ActivityLoginBinding;
import com.example.motow.rider.RiderActivity;
import com.example.motow.tower.TowerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        setListeners();
    }

    private void setListeners() {
        binding.signupRedirect.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.loginButton.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.loginButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.loginButton.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void signIn() {
        loading(true);
        String email = binding.loginEmail.getText().toString();
        String password = binding.loginPassword.getText().toString();

        fAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        checkUserAccessLevel(Objects.requireNonNull(task.getResult().getUser()).getUid());
                    } else {
                        loading(false);
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        if (binding.loginEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.loginEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.loginPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }

    private void checkUserAccessLevel(String uid) {
        DocumentReference df = fStore.collection("Users").document(uid);
        // extract the data from the document
        df.get().addOnSuccessListener(documentSnapshot -> {
            // identify the user access level
            if (documentSnapshot.getString("isAdmin") != null) {
                Intent intent = new Intent(getApplicationContext(), AdminActivity.class);
                startActivity(intent);
                finish();
            }
            if (documentSnapshot.getString("isRider") != null) {
                // User is a rider
                /*db.collection(Constants.KEY_COLLECTION_USERS)
                        .whereEqualTo(Constants.KEY_EMAIL, binding.loginEmail.getText().toString())
                        .whereEqualTo(Constants.KEY_PASSWORD, binding.loginPassword.getText().toString())
                        .whereEqualTo("isRider", "1")
                        .get()
                        .addOnCompleteListener(task -> {
                            if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                                DocumentSnapshot dfSnapshot = task.getResult().getDocuments().get(0);
                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                preferenceManager.putString(Constants.KEY_USER_ID, dfSnapshot.getId());
                                preferenceManager.putString(Constants.KEY_NAME, dfSnapshot.getString(Constants.KEY_NAME));
                                preferenceManager.putString(Constants.KEY_IMAGE, dfSnapshot.getString(Constants.KEY_IMAGE));
                            }
                        });*/
                Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
                startActivity(intent);
                finish();
            }
            if (documentSnapshot.getString("isTower") != null) {
                // User is a tower
                Intent intent = new Intent(getApplicationContext(), TowerActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            DocumentReference df = FirebaseFirestore.getInstance().collection("Users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
            df.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.getString("isRider") != null) {
                    startActivity(new Intent(getApplicationContext(), RiderActivity.class));
                    finish();
                }
                if (documentSnapshot.getString("isTower") != null) {
                    startActivity(new Intent(getApplicationContext(), TowerActivity.class));
                    finish();
                }
            }).addOnFailureListener(e -> {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            });
        }
    }
}