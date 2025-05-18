package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItemRequest {
    @JsonProperty("product_id")
    public int productId;
    @JsonProperty("quantity")
    public int quantity;
}
