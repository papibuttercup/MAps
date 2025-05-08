package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Button;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.Glide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ListedItemsActivity extends AppCompatActivity {

    private ImageView btnBackListedItems;
    private Button btnListNewItem;
    private RecyclerView recyclerView;
    private ListedItemsAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView txtTotalItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listed_items); // Use the new layout file

        btnBackListedItems = findViewById(R.id.btnBackListedItems);
        btnListNewItem = findViewById(R.id.btnListNewItem);
        btnBackListedItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle back button click (e.g., finish the activity)
                finish();
            }
        });
        btnListNewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ListedItemsActivity.this, ListNewItemActivity.class);
                startActivity(intent);
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListedItemsAdapter(productList);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        txtTotalItems = findViewById(R.id.txtTotalItems);
        loadSellerProducts();

        // You'll also need to find and set listeners for the
        // "List New Item" button and potentially handle displaying
        // the actual list of items (likely using a RecyclerView and an Adapter).
    }

    private void loadSellerProducts() {
        String sellerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        db.collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        productList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            productList.add(product);
                        }
                        adapter.notifyDataSetChanged();
                        txtTotalItems.setText(String.valueOf(productList.size()));
                    }
                });
    }
}

// Product model class
class Product {
    public String name, description, category, imageUrl, sellerId, sellerName;
    public double price;
    public int stock;
    public boolean isAvailable;
    public long createdAt;
    public Product() {}
}

// RecyclerView Adapter
class ListedItemsAdapter extends RecyclerView.Adapter<ListedItemsAdapter.ViewHolder> {
    private List<Product> products;
    public ListedItemsAdapter(List<Product> products) { this.products = products; }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listed_product, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.txtProductName.setText(product.name);
        holder.txtProductPrice.setText("Php " + product.price);
        holder.txtProductStatus.setText(product.isAvailable ? "Active" : "Inactive");
        // Load image with Glide or placeholder
        if (product.imageUrl != null && !product.imageUrl.isEmpty()) {
            Glide.with(holder.imgProduct.getContext())
                .load(product.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_image_placeholder);
        }
    }
    @Override
    public int getItemCount() { return products.size(); }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtProductName, txtProductPrice, txtProductStatus;
        public ViewHolder(View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductPrice = itemView.findViewById(R.id.txtProductPrice);
            txtProductStatus = itemView.findViewById(R.id.txtProductStatus);
        }
    }
}