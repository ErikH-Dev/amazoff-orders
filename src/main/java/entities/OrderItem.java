package entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "App_OrderItem")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "order_Id", referencedColumnName = "id", nullable = false)
    @JsonBackReference
    @JsonProperty("order")
    private Order order;

    @NotNull(message = "Product name must not be null")
    @Size(max=255, message = "Product name must be less than 255 characters")
    @JsonProperty("name")
    private String name;

    @Positive(message = "Product price must be positive")
    @NotNull(message = "Product price must not be null")
    @JsonProperty("price")
    private Double price;

    @NotNull
    @Size(max=255, message = "Product description must be less than 255 characters")
    @JsonProperty("description")
    private String description;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    @JsonProperty("quantity")
    private int quantity;

    public OrderItem() {}

    public OrderItem(String name, double price, String description, int quantity) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.quantity = quantity;
    }

    public OrderItem(int id, Order order, String name, double price, String description, int quantity) {
        this.id = id;
        this.order = order;
        this.name = name;
        this.price = price;
        this.description = description;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}