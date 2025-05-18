package dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateOrderRequest {
    @NotNull(message = "Buyer ID must not be null")
    @JsonProperty("oauth_id")
    public int oauthId;

    @NotEmpty(message = "Order must contain at least one item")
    @JsonProperty("order_items")
    public List<OrderItemRequest> orderItems = new ArrayList<>();

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(int oauthId, List<OrderItemRequest> orderItems) {
        this.oauthId = oauthId;
        this.orderItems = orderItems;
    }
}