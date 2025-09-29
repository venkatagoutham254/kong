package aforo.kong.dto;

import lombok.Data;

@Data
public class ClientApiDetailsDTO {
            private String name; // optional
        private String description; // optional
        @com.fasterxml.jackson.annotation.JsonProperty("baseUrl")
    @com.fasterxml.jackson.annotation.JsonAlias({"baseURL","base_url"})
    private String baseUrl;
        @com.fasterxml.jackson.annotation.JsonProperty("endpoint")
    @com.fasterxml.jackson.annotation.JsonAlias({"endpoint","path"})
    private String endpoint;
    @com.fasterxml.jackson.annotation.JsonProperty("authToken")
    @com.fasterxml.jackson.annotation.JsonAlias({"auth_token"})
    private String authToken;
}