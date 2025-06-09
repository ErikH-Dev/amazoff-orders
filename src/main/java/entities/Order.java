package entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import dto.BuyerDTO;
import enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "App_Order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private int id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotEmpty(message = "Order must contain at least one order item")
    @JsonManagedReference
    @JsonProperty("order_items")
    private List<OrderItem> orderItems = new ArrayList<>();

    @NotNull(message = "Order status must not be null")
    @Enumerated(EnumType.STRING)
    @JsonProperty("status")
    private OrderStatus status;

    @NotNull(message = "Order date must not be null")
    @JsonProperty("order_date")
    private LocalDateTime orderDate;

    @Column(name = "keycloak_id", nullable = false)
    @JsonProperty("keycloak_id")
    private String keycloakId;

    @Transient
    @JsonProperty("buyer")
    private BuyerDTO buyer;

    public Order() {
    }

    public Order(BuyerDTO buyer, List<OrderItem> orderItems, OrderStatus status, LocalDateTime orderDate) {
        this.buyer = buyer;
        this.orderItems = orderItems;
        this.status = status;
        this.orderDate = orderDate;
    }

    public Order(String keycloakId, List<OrderItem> orderItems, OrderStatus status, LocalDateTime orderDate) {
        this.keycloakId = keycloakId;
        this.orderItems = orderItems;
        this.status = status;
        this.orderDate = orderDate;
    }

    public Order(BuyerDTO buyer, String keycloakId, List<OrderItem> orderItems, OrderStatus status, LocalDateTime orderDate) {
        this.buyer = buyer;
        this.keycloakId = keycloakId;
        this.orderItems = orderItems;
        this.status = status;
        this.orderDate = orderDate;
    }

    public Order(int id, BuyerDTO buyer, List<OrderItem> orderItems, OrderStatus status, LocalDateTime orderDate) {
        this.id = id;
        this.buyer = buyer;
        this.orderItems = orderItems;
        this.status = status;
        this.orderDate = orderDate;
    }

    public int getId() {
        return id;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setBuyer(BuyerDTO buyer) {
        this.buyer = buyer;
    }

    public BuyerDTO getBuyer() {
        return buyer;
    }
}