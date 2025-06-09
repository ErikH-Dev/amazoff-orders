package dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateOrderRequest {
    @NotEmpty(message = "Order must contain at least one item")
    @JsonProperty("order_items")
    public List<OrderItemRequest> orderItems = new ArrayList<>();

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }
}