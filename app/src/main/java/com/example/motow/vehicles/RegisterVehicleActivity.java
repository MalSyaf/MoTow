package com.example.motow.vehicles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.motow.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterVehicleActivity extends AppCompatActivity {

    ImageView backBtn;
    EditText plateNum, brand, model, color;
    Button registerBtn, resetBtn;
    FirebaseAuth fAuth = FirebaseAuth.getInstance();
    FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    String userId, vehicleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_vehicle);

        backBtn = findViewById(R.id.back_btn);

        plateNum = findViewById(R.id.register_plate);
        brand = findViewById(R.id.register_brand);
        model = findViewById(R.id.register_model);
        color = findViewById(R.id.register_color);

        registerBtn = findViewById(R.id.register_btn);
        resetBtn = findViewById(R.id.reset_btn);

        userId = fAuth.getCurrentUser().getUid();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ManageVehicleActivity.class));
                finish();
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String plateFilled = plateNum.getText().toString();
                String brandFilled = brand.getText().toString();
                String modelFilled = model.getText().toString();
                String colorFilled = color.getText().toString();

                if (TextUtils.isEmpty(plateFilled)) {
                    Toast.makeText(RegisterVehicleActivity.this, "Enter Plate Number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(brandFilled)) {
                    Toast.makeText(RegisterVehicleActivity.this, "Enter Brand", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(modelFilled)) {
                    Toast.makeText(RegisterVehicleActivity.this, "Enter Model", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(colorFilled)) {
                    Toast.makeText(RegisterVehicleActivity.this, "Enter Color", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> vehicle = new HashMap<>();
                vehicle.put("ownerId", userId);
                vehicle.put("plateNumber", plateFilled);
                vehicle.put("brand", brandFilled);
                vehicle.put("model", modelFilled);
                vehicle.put("color", colorFilled);

                fStore.collection("Vehicles")
                        .add(vehicle)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Toast.makeText(RegisterVehicleActivity.this, "Vehicle Registered", Toast.LENGTH_SHORT).show();
                                plateNum.setText(null);
                                brand.setText(null);
                                model.setText(null);
                                color.setText(null);
                                String documentId = documentReference.getId();
                                Map<String, Object> vehicleId = new HashMap<>();
                                vehicleId.put("vehicleId", documentId);
                                fStore.collection("Vehicles")
                                        .document(documentId)
                                        .update(vehicleId);
                            }
                        });
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                plateNum.setText(null);
                brand.setText(null);
                model.setText(null);
                color.setText(null);
            }
        });
    }
}