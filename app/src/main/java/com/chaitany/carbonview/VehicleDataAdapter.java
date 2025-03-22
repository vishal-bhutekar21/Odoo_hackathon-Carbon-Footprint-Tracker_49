package com.chaitany.carbonview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class VehicleDataAdapter extends RecyclerView.Adapter<VehicleDataAdapter.ViewHolder> {
    private List<VehicleData> vehicleDataList;

    public VehicleDataAdapter(List<VehicleData> vehicleDataList) {
        this.vehicleDataList = vehicleDataList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        VehicleData data = vehicleDataList.get(position);
        holder.vehicleIdText.setText(data.getVehicleId());
        holder.kmTraveledText.setText(String.format(Locale.getDefault(), "%,d km", data.getKmTraveled()));
        holder.fuelTypeText.setText(data.getFuelType());
        holder.emissionsText.setText(String.format(Locale.getDefault(), "%.1f tonnes", data.getEmissions()));
    }

    @Override
    public int getItemCount() {
        return vehicleDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView vehicleIdText;
        public TextView kmTraveledText;
        public TextView fuelTypeText;
        public TextView emissionsText;

        public ViewHolder(View view) {
            super(view);
            vehicleIdText = view.findViewById(R.id.vehicleIdText);
            kmTraveledText = view.findViewById(R.id.kmTraveledText);
            fuelTypeText = view.findViewById(R.id.fuelTypeText);
            emissionsText = view.findViewById(R.id.emissionsText);
        }
    }
}