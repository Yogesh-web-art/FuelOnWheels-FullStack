package com.example.fuelonwheelsapp.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fuelonwheelsapp.R;

import java.util.ArrayList;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {
    private final ArrayList<ProfileItem> profileItems;
    private OnItemClickListener onItemClickListener = null;

    public interface OnItemClickListener {
        void onClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView textView;
        private final TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.profile_item_imageView);
            textView = view.findViewById(R.id.profile_item_textView);
            subtitleView = view.findViewById(R.id.profile_item_subtitle);
            view.setOnClickListener(view1 -> {
                if (onItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClickListener.onClick(view1, getAdapterPosition());
                }
            });
        }

        public ImageView getImageView() {
            return imageView;
        }

        public TextView getTextView() {
            return textView;
        }

        public TextView getSubtitleView() {
            return subtitleView;
        }
    }

    public ProfileAdapter(ArrayList<ProfileItem> dataSet, OnItemClickListener clickListener) {
        this.profileItems = dataSet;
        this.onItemClickListener = clickListener;
    }

    @NonNull
    @Override
    public ProfileAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.profile_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileAdapter.ViewHolder holder, int position) {
        ProfileItem item = profileItems.get(position);
        holder.getImageView().setImageResource(item.getIconId());
        holder.getTextView().setText(item.getItem());
        if (holder.getSubtitleView() != null && item.getSubtitle() != null && !item.getSubtitle().isEmpty()) {
            holder.getSubtitleView().setText(item.getSubtitle());
            holder.getSubtitleView().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return profileItems.size();
    }
}
