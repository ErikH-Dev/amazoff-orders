package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductDTO {
    @JsonProperty("product_id")
    public int id;
    @JsonProperty("name")
    public String name;
    @JsonProperty("description")
    public String description;
    @JsonProperty("price")
    public Double price;
}
