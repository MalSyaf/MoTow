package com.example.motow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class VehicleAdapter extends FirestoreRecyclerAdapter<Vehicle, VehicleAdapter.MyViewHolder> {

    /**
     * Create a new RecyclerView adapter that listens to a Firestore Query.  See {@link
     * FirestoreRecyclerOptions} for configuration options.
     *
     * @param options
     */
    public VehicleAdapter(@NonNull FirestoreRecyclerOptions<Vehicle> options) {
        super(options);
    }

    @NonNull
    @Override
    public VehicleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.vehicle_card_view, parent, false);
        return new MyViewHolder(v);
    }

    public void deleteItem(int position) {
        getSnapshots().getSnapshot(position).getReference().delete();
    }

    @Override
    protected void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull Vehicle model) {

        holder.plateNum.setText(model.plateNumber);
        holder.brand.setText(model.brand);
        holder.model.setText(model.model);
        holder.color.setText(model.color);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView plateNum, brand, model, color;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            plateNum = itemView.findViewById(R.id.display_plate);
            brand = itemView.findViewById(R.id.display_brand);
            model = itemView.findViewById(R.id.display_model);
            color = itemView.findViewById(R.id.display_color);

        }
    }
}
