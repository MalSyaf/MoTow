package com.example.motow.vehicles;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.motow.R;
import com.example.motow.utilities.Constants;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class VehicleAdapter extends FirestoreRecyclerAdapter<Vehicle, VehicleAdapter.MyViewHolder> {

    public static OnItemClickListener listener;

    public VehicleAdapter(@NonNull FirestoreRecyclerOptions<Vehicle> options) {
        super(options);
    }

    @NonNull
    @Override
    public VehicleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_card_view, parent, false);
        return new MyViewHolder(v);
    }
    
    @Override
    protected void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull Vehicle model) {
        holder.plateNum.setText(model.plateNumber);
        holder.brand.setText(model.brand);
        holder.model.setText(model.model);
        holder.color.setText(model.color);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        // Firebase
        private FirebaseAuth fAuth;
        private FirebaseFirestore fStore;

        // Interface
        private ImageView vehicleImage;
        private TextView plateNum, brand, model, color;
        private MaterialCardView vehicleContainer;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            // Firebase
            fAuth = FirebaseAuth.getInstance();
            fStore = FirebaseFirestore.getInstance();
            String userId = fAuth.getCurrentUser().getUid();

            // Interface
            vehicleImage = itemView.findViewById(R.id.vehicleImage);
            plateNum = itemView.findViewById(R.id.display_plate);
            brand = itemView.findViewById(R.id.display_brand);
            model = itemView.findViewById(R.id.display_model);
            color = itemView.findViewById(R.id.display_color);
            vehicleContainer = itemView.findViewById(R.id.vehicle_container);

            fStore.collection("Users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if(documentSnapshot.getString("isRider") != null) {
                                vehicleImage.setImageResource(R.drawable.sportbike);
                            }
                            if(documentSnapshot.getString("isTower") != null) {
                                vehicleImage.setImageResource(R.drawable.main);
                            }
                        }
                    });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(position!=RecyclerView.NO_POSITION && listener != null){
                        listener.onItemClick(getSnapshots().getSnapshot(position), position);
                        vehicleContainer.setStrokeWidth(4);
                        vehicleContainer.setStrokeColor(Color.BLACK);
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
