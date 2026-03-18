package com.dashboard.oauth.dataTransferObject.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntrospectionResponse {

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("sub")
    private String sub;

    /** Unix timestamp in seconds (RFC 7519 §4.1.4) */
    @JsonProperty("exp")
    private Long exp;

    /** Space-separated list of granted permissions (RFC 6749 §3.3) */
    @JsonProperty("scope")
    private String scope;
}
