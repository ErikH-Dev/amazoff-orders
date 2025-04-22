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
    @JsonProperty("order_item_ids")
    public List<Integer> orderItemsIds = new ArrayList<>();

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(int oauthId, List<Integer> orderItemsIds) {
        this.oauthId = oauthId;
        this.orderItemsIds = orderItemsIds;
    }
}