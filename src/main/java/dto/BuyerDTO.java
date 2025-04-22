package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuyerDTO {
    @JsonProperty("oauth_id")
    public int oauthId;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("email")
    public String email;
}
