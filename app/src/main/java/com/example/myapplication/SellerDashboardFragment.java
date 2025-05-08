package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class SellerDashboardFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seller_dashboard, container, false);

        setupRow(view, R.id.row_listed_items, "Listed Items", ListedItemsActivity.class);
        setupRow(view, R.id.row_total_sales, "Total Sales", TotalSalesActivity.class);
        setupRow(view, R.id.row_total_views, "Total Views", TotalViewsActivity.class);
        setupRow(view, R.id.row_orders, "Orders", OrderDetailsActivity.class);
        setupRow(view, R.id.row_reviews, "Reviews", ReviewsActivity.class);

        return view;
    }

    private void setupRow(View parentView, int rowId, String title, Class<?> targetActivity) {
        View row = parentView.findViewById(rowId);
        row.setOnClickListener(v -> startActivity(new Intent(getActivity(), targetActivity)));

        TextView titleView = row.findViewById(R.id.menuText);
        titleView.setText(title);
    }
} 