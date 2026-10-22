package com.example.fuelonwheelsapp.profile.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelonwheelsapp.R;

import java.util.ArrayList;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder>{
    private final ArrayList<Order> orderItems;
    private OnItemClickListener onItemClickListener = null;

    public interface OnItemClickListener{
        void onClick(View view, int position);
    }

    public OrderListAdapter(ArrayList<Order> orderItems, OnItemClickListener onItemClickListener) {
        this.orderItems = orderItems;
        this.onItemClickListener = onItemClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView orderId;
        private final TextView dateTime;
        private final TextView toAddress;
        private final TextView fromAddress;
        private final TextView amount;

        public ViewHolder(View view) {
            super(view);
            dateTime = (TextView) view.findViewById(R.id.tv_dateTime);
            toAddress = (TextView) view.findViewById(R.id.tv_to_address);
            fromAddress = (TextView) view.findViewById(R.id.tv_from_address);
            amount = (TextView) view.findViewById(R.id.tv_amount);
            orderId = (TextView) view.findViewById(R.id.tv_orderId);
            view.setOnClickListener(view1 -> {
                if (onItemClickListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClickListener.onClick(view1, pos);
                    }
                }
            });
        }

        public TextView getDateTime() { return dateTime;}
        public TextView getToAddress() { return toAddress; }
        public TextView getFromAddress() { return fromAddress; }
        public TextView getAmount() { return amount; }
        public TextView getOrderId() { return orderId; }
    }

    @NonNull
    @Override
    public OrderListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.order_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderListAdapter.ViewHolder holder, int position) {
        Order item = orderItems.get(position);
        if (item == null) return;

        String from = item.getFuelLocation() != null ? item.getFuelLocation() : "Fuel Station Depot";
        String to = item.getUserLocation() != null ? item.getUserLocation() : "Delivery Location";
        String amount = item.getTotalAmount() != null ? item.getTotalAmount() : "0";
        if (!amount.startsWith("₹") && !amount.startsWith("Rs")) {
            amount = "₹" + amount;
        }
        String date = item.getDateTime() != null ? item.getDateTime() : "Recent Order";
        String id = item.getOrderId() != null ? item.getOrderId() : "#ORD-N/A";
        if (!id.startsWith("#")) {
            id = "#" + id;
        }

        holder.getFromAddress().setText(from);
        holder.getToAddress().setText(to);
        holder.getAmount().setText(amount);
        holder.getDateTime().setText(date);
        holder.getOrderId().setText(id);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }
}
