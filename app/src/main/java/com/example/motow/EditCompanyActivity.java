package com.example.motow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class EditCompanyActivity extends AppCompatActivity {

    // Firebase
    FirebaseAuth fAuth = FirebaseAuth.getInstance();
    FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    String userId = fAuth.getCurrentUser().getUid();
    String companyId;

    // Interface
    ImageView backBtn;
    EditText companyName, regNum;
    CheckBox isPrivate, isCompany;
    Button registerBtn, resetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_company);

        backBtn = findViewById(R.id.back_btn);
        companyName = findViewById(R.id.reg_company);
        regNum = findViewById(R.id.reg_regNum);
        isPrivate = findViewById(R.id.private_cbox);
        isCompany = findViewById(R.id.company_cbox);
        registerBtn = findViewById(R.id.register_btn);
        resetBtn = findViewById(R.id.reset_btn);


        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), UserInfoActivity.class);
                startActivity(intent);
                finish();
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String companyNameFilled = companyName.getText().toString();
                String regNumFilled = regNum.getText().toString();

                if (TextUtils.isEmpty(companyNameFilled)) {
                    Toast.makeText(EditCompanyActivity.this, "Enter Company Name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(regNumFilled)) {
                    Toast.makeText(EditCompanyActivity.this, "Enter Registration Number", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Checkbox validation
                if(!(isPrivate.isChecked() || isCompany.isChecked())) {
                    Toast.makeText(EditCompanyActivity.this, "Select Business Type", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> companyInfo = new HashMap<>();

                fStore.collection("Company")
                        .whereEqualTo("registrationNumber", regNumFilled)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                Toast.makeText(EditCompanyActivity.this, "Company is existed", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                companyInfo.put("companyName", companyNameFilled);
                                companyInfo.put("registrationNumber", regNumFilled);
                            }
                        });


                fStore.collection("Company")
                        .add(companyInfo)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Toast.makeText(EditCompanyActivity.this, "Company is Registered", Toast.LENGTH_SHORT).show();
                                companyName.setText(null);
                                regNum.setText(null);
                                isPrivate.setChecked(false);
                                isCompany.setChecked(false);

                                Map<String, Object> updateInfo= new HashMap<>();
                                if(isPrivate.isChecked()){
                                    updateInfo.put("companyId", documentReference.getId());
                                    updateInfo.put("providerType", "private");
                                }
                                if(isCompany.isChecked()){
                                    updateInfo.put("companyId", documentReference.getId());
                                    updateInfo.put("providerType", "company");
                                }

                                fStore.collection("Users")
                                        .document(userId)
                                        .update(updateInfo);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(EditCompanyActivity.this, "Company Register Unsuccessful", Toast.LENGTH_SHORT).show();
                            }
                        });
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

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                companyName.setText(null);
                regNum.setText(null);
                isPrivate.setChecked(false);
                isCompany.setChecked(false);
            }
        });
    }
}