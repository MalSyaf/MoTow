package com.example.motow;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.motow.databinding.ActivityUserInfoBinding;
import com.example.motow.rider.RiderManageActivity;
import com.example.motow.tower.TowerManageActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    private ActivityUserInfoBinding binding;

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        setListeners();
        loadLayout();
        displayUserInfo();
    }

    private void setListeners() {
        binding.backBtn.setOnClickListener(view -> {
            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            // identify the user access level
                            if (documentSnapshot.getString("isRider") != null) {
                                // user is a rider
                                Intent intent = new Intent(getApplicationContext(), RiderManageActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            if (documentSnapshot.getString("isTower") != null) {
                                // user is a rider
                                Intent intent = new Intent(getApplicationContext(), TowerManageActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        });

        // Checkbox logic
        binding.privateCbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    binding.companyCbox.setChecked(false);
                }
            }
        });
        binding.companyCbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    binding.privateCbox.setChecked(false);
                }
            }
        });

        binding.manageInfoBtn.setOnClickListener(view -> {
            interfaceSetup();

            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            binding.editName.setText(documentSnapshot.getString("name"));
                            binding.editEmail.setHint(documentSnapshot.getString("email"));
                            binding.editPhone.setText(documentSnapshot.getString("contact"));
                            binding.editCompany.setText(documentSnapshot.getString("companyName"));
                            binding.editRegnum.setText(documentSnapshot.getString("companyRegNum"));

                            fStore.collection("Users")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("providerType", "Company")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if(task.isSuccessful()){
                                                    binding.companyCbox.setChecked(true);
                                            }
                                        }
                                    });
                            fStore.collection("Users")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("providerType", "Private")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if(task.isSuccessful()){
                                                    binding.privateCbox.setChecked(true);
                                            }
                                        }
                                    });
                        }
                    });
        });

        binding.cancelBtn.setOnClickListener(view -> {
            showAlertDialog();
        });

        binding.saveBtn.setOnClickListener(view -> {
            String nameFilled = binding.editName.getText().toString();
            String phoneFilled = binding.editPhone.getText().toString();
            String companyNameFilled = binding.editCompany.getText().toString();
            String regNumFilled = binding.editRegnum.getText().toString();

            // Update user info
            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.getString("isRider") != null) {
                                // User is a rider
                                if (TextUtils.isEmpty(nameFilled)) {
                                    Toast.makeText(UserInfoActivity.this, "Enter Full Name", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (TextUtils.isEmpty(phoneFilled)) {
                                    Toast.makeText(UserInfoActivity.this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                binding.backBtn.setVisibility(View.VISIBLE);
                                binding.manageInfoBtn.setVisibility(View.VISIBLE);
                                binding.nameLayout.setVisibility(View.VISIBLE);
                                binding.emailLayout.setVisibility(View.VISIBLE);
                                binding.phoneLayout.setVisibility(View.VISIBLE);

                                binding.saveBtn.setVisibility(View.GONE);
                                binding.cancelBtn.setVisibility(View.GONE);
                                binding.editName.setVisibility(View.GONE);
                                binding.editEmail.setVisibility(View.GONE);
                                binding.editPhone.setVisibility(View.GONE);

                                Map<String, Object> updateUser = new HashMap<>();
                                updateUser.put("name", nameFilled);
                                updateUser.put("contact", phoneFilled);

                                fStore.collection("Users")
                                        .document(userId)
                                        .update(updateUser)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(UserInfoActivity.this, "Information has been updated", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                            if (documentSnapshot.getString("isTower") != null) {
                                // User is a tower
                                if (TextUtils.isEmpty(nameFilled)) {
                                    Toast.makeText(UserInfoActivity.this, "Enter Full Name", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (TextUtils.isEmpty(phoneFilled)) {
                                    Toast.makeText(UserInfoActivity.this, "Enter Phone Number", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (TextUtils.isEmpty(companyNameFilled)) {
                                    Toast.makeText(UserInfoActivity.this, "Enter Company Name", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (TextUtils.isEmpty(regNumFilled)) {
                                    Toast.makeText(UserInfoActivity.this, "Enter Registration Number", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Checkbox validation
                                if(!(binding.privateCbox.isChecked() || binding.companyCbox.isChecked())) {
                                    Toast.makeText(UserInfoActivity.this, "Select Business Type", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                binding.backBtn.setVisibility(View.VISIBLE);
                                binding.manageInfoBtn.setVisibility(View.VISIBLE);
                                binding.nameLayout.setVisibility(View.VISIBLE);
                                binding.emailLayout.setVisibility(View.VISIBLE);
                                binding.phoneLayout.setVisibility(View.VISIBLE);
                                binding.typeLayout.setVisibility(View.VISIBLE);
                                binding.companyLayout.setVisibility(View.VISIBLE);

                                binding.saveBtn.setVisibility(View.GONE);
                                binding.cancelBtn.setVisibility(View.GONE);
                                binding.editName.setVisibility(View.GONE);
                                binding.editEmail.setVisibility(View.GONE);
                                binding.editPhone.setVisibility(View.GONE);
                                binding.editCompany.setVisibility(View.GONE);
                                binding.editRegnum.setVisibility(View.GONE);
                                binding.privateCbox.setVisibility(View.GONE);
                                binding.privateCbox.setVisibility(View.GONE);

                                Map<String, Object> updateUser = new HashMap<>();
                                updateUser.put("name", nameFilled);
                                updateUser.put("contact", phoneFilled);
                                updateUser.put("companyName", companyNameFilled);
                                updateUser.put("companyRegNum", regNumFilled);
                                if(binding.privateCbox.isChecked()){
                                    updateUser.put("providerType", "Private");
                                }
                                if(binding.companyCbox.isChecked()){
                                    updateUser.put("providerType", "Company");
                                }

                                fStore.collection("Users")
                                        .document(userId)
                                        .update(updateUser)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(UserInfoActivity.this, "Information has been updated", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    });
            closeKeyboard();
        });
    }

    private void loadLayout() {
        // extract the data from the document
        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        // identify the user access level
                        if (documentSnapshot.getString("isRider") != null) {
                            // user is a rider
                            binding.typeLayout.setVisibility(View.GONE);
                            binding.companyLayout.setVisibility(View.GONE);
                        }
                        if (documentSnapshot.getString("isTower") != null) {
                            // user is a tower
                            binding.typeLayout.setVisibility(View.VISIBLE);
                            binding.companyLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void displayUserInfo() {
        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                binding.name.setText(value.getString("name"));
                binding.email.setText(value.getString("email"));
                binding.phoneNo.setText(value.getString("contact"));
                binding.company.setText(value.getString("companyName"));
                binding.company.setTextColor(getResources().getColor(R.color.grey_btn));
                binding.type.setText(value.getString("providerType"));
                binding.type.setTextColor(getResources().getColor(R.color.grey_btn));
            }
        });
    }

    private void showAlertDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Are you sure?");
        alert.setMessage("Do you want to continue without saving?");
        alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                binding.saveBtn.setVisibility(View.GONE);
                binding.cancelBtn.setVisibility(View.GONE);
                binding.editName.setVisibility(View.GONE);
                binding.editEmail.setVisibility(View.GONE);
                binding.editPhone.setVisibility(View.GONE);
                binding.editCompany.setVisibility(View.GONE);
                binding.editRegnum.setVisibility(View.GONE);
                binding.privateCbox.setVisibility(View.GONE);
                binding.companyCbox.setVisibility(View.GONE);

                fStore.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.getString("isRider") != null) {
                                    // user is a rider
                                    binding.backBtn.setVisibility(View.VISIBLE);
                                    binding.manageInfoBtn.setVisibility(View.VISIBLE);
                                    binding.nameLayout.setVisibility(View.VISIBLE);
                                    binding.emailLayout.setVisibility(View.VISIBLE);
                                    binding.phoneLayout.setVisibility(View.VISIBLE);
                                }
                                if (documentSnapshot.getString("isTower") != null) {
                                    // user is a tower
                                    binding.backBtn.setVisibility(View.VISIBLE);
                                    binding.manageInfoBtn.setVisibility(View.VISIBLE);
                                    binding.nameLayout.setVisibility(View.VISIBLE);
                                    binding.emailLayout.setVisibility(View.VISIBLE);
                                    binding.phoneLayout.setVisibility(View.VISIBLE);
                                    binding.typeLayout.setVisibility(View.VISIBLE);
                                    binding.companyLayout.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        });
        alert.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //
            }
        });
        alert.create().show();
        closeKeyboard();
    }

    private void interfaceSetup() {
        binding.backBtn.setVisibility(View.GONE);
        binding.manageInfoBtn.setVisibility(View.GONE);
        binding.nameLayout.setVisibility(View.GONE);
        binding.emailLayout.setVisibility(View.GONE);
        binding.phoneLayout.setVisibility(View.GONE);
        binding.typeLayout.setVisibility(View.GONE);
        binding.companyLayout.setVisibility(View.GONE);

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.getString("isRider") != null) {
                            // user is a rider
                            binding.saveBtn.setVisibility(View.VISIBLE);
                            binding.cancelBtn.setVisibility(View.VISIBLE);
                            binding.editName.setVisibility(View.VISIBLE);
                            binding.editEmail.setVisibility(View.VISIBLE);
                            binding.editPhone.setVisibility(View.VISIBLE);
                        }
                        if (documentSnapshot.getString("isTower") != null) {
                            // user is a tower
                            binding.saveBtn.setVisibility(View.VISIBLE);
                            binding.cancelBtn.setVisibility(View.VISIBLE);
                            binding.editName.setVisibility(View.VISIBLE);
                            binding.editEmail.setVisibility(View.VISIBLE);
                            binding.editPhone.setVisibility(View.VISIBLE);
                            binding.editCompany.setVisibility(View.VISIBLE);
                            binding.editRegnum.setVisibility(View.VISIBLE);
                            binding.privateCbox.setVisibility(View.VISIBLE);
                            binding.companyCbox.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}