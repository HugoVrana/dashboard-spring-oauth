package com.dashboard.oauth.dataTransferObject.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntrospectionResponse {

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("iss")
    private String iss;

    @JsonProperty("aud")
    private String aud;

    @JsonProperty("client_id")
    private String clientId;

    /** Unix timestamp in seconds (RFC 7519 §4.1.4) */
    @JsonProperty("exp")
    private Long exp;

    /** Unix timestamp in seconds (RFC 7519 §4.1.6) */
    @JsonProperty("iat")
    private Long iat;

    /** Space-separated list of granted permissions (RFC 6749 §3.3) */
    @JsonProperty("scope")
    private String scope;
}
