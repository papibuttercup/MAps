package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity {
    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<OrderItem> orderList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView ordersTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ordersTitle = findViewById(R.id.ordersTitle);
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter(orderList);
        ordersRecyclerView.setAdapter(ordersAdapter);

        loadSellerOrders();
    }

    private void loadSellerOrders() {
        String sellerId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (sellerId == null) return;
        db.collection("orders")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                orderList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String userId = doc.getString("userId");
                    String productId = doc.getString("productId");
                    String status = doc.getString("status");
                    // Fetch user name and product name
                    fetchUserAndProduct(userId, productId, status);
                }
            });
    }

    private void fetchUserAndProduct(String userId, String productId, String status) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.exists() ? userDoc.getString("firstName") + " " + userDoc.getString("lastName") : "Unknown User";
            db.collection("products").document(productId).get().addOnSuccessListener(productDoc -> {
                String productName = productDoc.exists() ? productDoc.getString("name") : "Unknown Product";
                orderList.add(new OrderItem(userName, productName, status));
                ordersAdapter.notifyDataSetChanged();
            });
        });
    }

    // Order item model for display
    static class OrderItem {
        String userName;
        String productName;
        String status;
        OrderItem(String userName, String productName, String status) {
            this.userName = userName;
            this.productName = productName;
            this.status = status;
        }
    }

    // RecyclerView Adapter
    static class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
        private final List<OrderItem> orders;
        OrdersAdapter(List<OrderItem> orders) { this.orders = orders; }
        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(), R.layout.item_order, null);
            return new OrderViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            OrderItem item = orders.get(position);
            holder.userName.setText(item.userName);
            holder.productName.setText(item.productName);
            holder.status.setText("Status: " + (item.status != null ? item.status : "Pending"));
        }
        @Override
        public int getItemCount() { return orders.size(); }
        static class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView userName, productName, status;
            OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                userName = itemView.findViewById(R.id.orderUserName);
                productName = itemView.findViewById(R.id.orderProductName);
                status = itemView.findViewById(R.id.orderStatus);
            }
        }
    }
} 