package com.example.motow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.MyViewHolder> {

    Context context;
    ArrayList<Vehicle> vehicleArrayList;

    public VehicleAdapter(Context context, ArrayList<Vehicle> vehicleArrayList) {
        this.context = context;
        this.vehicleArrayList = vehicleArrayList;
    }

    @NonNull
    @Override
    public VehicleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(context).inflate(R.layout.vehicle_card_view, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleAdapter.MyViewHolder holder, int position) {

        Vehicle vehicle = vehicleArrayList.get(position);

        holder.plateNum.setText(vehicle.plateNumber);
        holder.brand.setText(vehicle.brand);
        holder.model.setText(vehicle.model);
        holder.color.setText(vehicle.color);
    }

    @Override
    public int getItemCount() {
        return vehicleArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView plateNum, brand, model, color;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            plateNum = itemView.findViewById(R.id.display_plate);
            brand = itemView.findViewById(R.id.display_brand);
            model = itemView.findViewById(R.id.display_model);
            color =itemView.findViewById(R.id.display_color);

        }
    }
}
