package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuyerDTO {
    @JsonProperty("keycloak_id")
    public String keycloakId;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("email")
    public String email;
}
