package com.example.myapplication;

public class Product {
    private String id;
    private String name;
    private String imageUrl;
    private double price;
    private String sellerId;
    private boolean isAvailable;
    private int stock;

    public Product() {}

    public Product(String id, String name, String imageUrl, double price, String sellerId, boolean isAvailable, int stock) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
        this.sellerId = sellerId;
        this.isAvailable = isAvailable;
        this.stock = stock;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
} 