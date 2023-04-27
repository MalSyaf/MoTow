package com.example.motow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;

    // Interface
    private RelativeLayout nameLayout, emailLayout, phoneLayout, companyLayout, typeLayout;
    private ImageView backBtn;
    private Button manageBtn, saveBtn, cancelBtn;
    private TextView fullName, email, phoneNo, companyName, providerType;
    private EditText editName, editEmail, editPhone, editCompany, editReg;
    private CheckBox isPrivate, isCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        // Firebase
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        // Company & provider
        typeLayout = findViewById(R.id.type_layout);
        providerType = findViewById(R.id.type);
        companyLayout = findViewById(R.id.company_layout);
        companyName = findViewById(R.id.company);

        // Edit Interface
        saveBtn = findViewById(R.id.save_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        editCompany = findViewById(R.id.edit_company);
        editReg = findViewById(R.id.edit_regnum);
        isPrivate = findViewById(R.id.private_cbox);
        isCompany = findViewById(R.id.company_cbox);

        // Layout
        nameLayout = findViewById(R.id.name_layout);
        emailLayout = findViewById(R.id.email_layout);
        phoneLayout = findViewById(R.id.phone_layout);

        //Interface
        backBtn = findViewById(R.id.back_btn);
        fullName = findViewById(R.id.name);
        email = findViewById(R.id.email);
        phoneNo = findViewById(R.id.phone_no);
        manageBtn = findViewById(R.id.manage_info_btn);

        DocumentReference df = fStore.collection("Users").document(userId);
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
                            typeLayout.setVisibility(View.GONE);
                            companyLayout.setVisibility(View.GONE);
                        }
                        if (documentSnapshot.getString("isTower") != null) {
                            // user is a tower
                            typeLayout.setVisibility(View.VISIBLE);
                            companyLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });

        DocumentReference documentReference = fStore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                fullName.setText(value.getString("fullName"));
                email.setText(value.getString("email"));
                phoneNo.setText(value.getString("phoneNumber"));
                companyName.setText(value.getString("companyName"));
                companyName.setTextColor(getResources().getColor(R.color.grey_btn));
                providerType.setText(value.getString("providerType"));
                providerType.setTextColor(getResources().getColor(R.color.grey_btn));
            }
        });

        manageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                interfaceSetup();

                fStore.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                editName.setText(documentSnapshot.getString("fullName"));
                                editEmail.setHint(documentSnapshot.getString("email"));
                                editPhone.setText(documentSnapshot.getString("phoneNumber"));
                                editCompany.setText(documentSnapshot.getString("companyName"));
                                editReg.setText(documentSnapshot.getString("companyRegNum"));

                                fStore.collection("Users")
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("providerType", "Company")
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if(task.isSuccessful()){
                                                    for(QueryDocumentSnapshot document: task.getResult()){
                                                        isCompany.setChecked(true);
                                                    }
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
                                                    for(QueryDocumentSnapshot document: task.getResult()){
                                                        isPrivate.setChecked(true);
                                                    }
                                                }
                                            }
                                        });
                            }
                        });
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertDialog();
            }
        });

        // Checkbox logic
        isPrivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    isCompany.setChecked(false);
                }
            }
        });
        isCompany.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    isPrivate.setChecked(false);
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nameFilled = editName.getText().toString();
                String phoneFilled = editPhone.getText().toString();
                String companyNameFilled = editCompany.getText().toString();
                String regNumFilled = editReg.getText().toString();

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

                                    backBtn.setVisibility(View.VISIBLE);
                                    manageBtn.setVisibility(View.VISIBLE);
                                    nameLayout.setVisibility(View.VISIBLE);
                                    emailLayout.setVisibility(View.VISIBLE);
                                    phoneLayout.setVisibility(View.VISIBLE);

                                    saveBtn.setVisibility(View.GONE);
                                    cancelBtn.setVisibility(View.GONE);
                                    editName.setVisibility(View.GONE);
                                    editEmail.setVisibility(View.GONE);
                                    editPhone.setVisibility(View.GONE);

                                    Map<String, Object> updateUser = new HashMap<>();
                                    updateUser.put("fullName", nameFilled);
                                    updateUser.put("phoneNumber", phoneFilled);

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
                                    if(!(isPrivate.isChecked() || isCompany.isChecked())) {
                                        Toast.makeText(UserInfoActivity.this, "Select Business Type", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    backBtn.setVisibility(View.VISIBLE);
                                    manageBtn.setVisibility(View.VISIBLE);
                                    nameLayout.setVisibility(View.VISIBLE);
                                    emailLayout.setVisibility(View.VISIBLE);
                                    phoneLayout.setVisibility(View.VISIBLE);
                                    typeLayout.setVisibility(View.VISIBLE);
                                    companyLayout.setVisibility(View.VISIBLE);

                                    saveBtn.setVisibility(View.GONE);
                                    cancelBtn.setVisibility(View.GONE);
                                    editName.setVisibility(View.GONE);
                                    editEmail.setVisibility(View.GONE);
                                    editPhone.setVisibility(View.GONE);
                                    editCompany.setVisibility(View.GONE);
                                    editReg.setVisibility(View.GONE);
                                    isPrivate.setVisibility(View.GONE);
                                    isCompany.setVisibility(View.GONE);

                                    Map<String, Object> updateUser = new HashMap<>();
                                    updateUser.put("fullName", nameFilled);
                                    updateUser.put("phoneNumber", phoneFilled);
                                    updateUser.put("companyName", companyNameFilled);
                                    updateUser.put("companyRegNum", regNumFilled);
                                    if(isPrivate.isChecked()){
                                        updateUser.put("providerType", "Private");
                                    }
                                    if(isCompany.isChecked()){
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
                saveBtn.setVisibility(View.GONE);
                cancelBtn.setVisibility(View.GONE);
                editName.setVisibility(View.GONE);
                editEmail.setVisibility(View.GONE);
                editPhone.setVisibility(View.GONE);
                editCompany.setVisibility(View.GONE);
                editReg.setVisibility(View.GONE);
                isPrivate.setVisibility(View.GONE);
                isCompany.setVisibility(View.GONE);

                fStore.collection("Users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.getString("isRider") != null) {
                                    // user is a rider
                                    backBtn.setVisibility(View.VISIBLE);
                                    manageBtn.setVisibility(View.VISIBLE);
                                    nameLayout.setVisibility(View.VISIBLE);
                                    emailLayout.setVisibility(View.VISIBLE);
                                    phoneLayout.setVisibility(View.VISIBLE);
                                }
                                if (documentSnapshot.getString("isTower") != null) {
                                    // user is a tower
                                    backBtn.setVisibility(View.VISIBLE);
                                    manageBtn.setVisibility(View.VISIBLE);
                                    nameLayout.setVisibility(View.VISIBLE);
                                    emailLayout.setVisibility(View.VISIBLE);
                                    phoneLayout.setVisibility(View.VISIBLE);
                                    typeLayout.setVisibility(View.VISIBLE);
                                    companyLayout.setVisibility(View.VISIBLE);
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
        backBtn.setVisibility(View.GONE);
        manageBtn.setVisibility(View.GONE);
        nameLayout.setVisibility(View.GONE);
        emailLayout.setVisibility(View.GONE);
        phoneLayout.setVisibility(View.GONE);
        typeLayout.setVisibility(View.GONE);
        companyLayout.setVisibility(View.GONE);

        fStore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.getString("isRider") != null) {
                            // user is a rider
                            saveBtn.setVisibility(View.VISIBLE);
                            cancelBtn.setVisibility(View.VISIBLE);
                            editName.setVisibility(View.VISIBLE);
                            editEmail.setVisibility(View.VISIBLE);
                            editPhone.setVisibility(View.VISIBLE);
                        }
                        if (documentSnapshot.getString("isTower") != null) {
                            // user is a tower
                            saveBtn.setVisibility(View.VISIBLE);
                            cancelBtn.setVisibility(View.VISIBLE);
                            editName.setVisibility(View.VISIBLE);
                            editEmail.setVisibility(View.VISIBLE);
                            editPhone.setVisibility(View.VISIBLE);
                            editCompany.setVisibility(View.VISIBLE);
                            editReg.setVisibility(View.VISIBLE);
                            isPrivate.setVisibility(View.VISIBLE);
                            isCompany.setVisibility(View.VISIBLE);
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