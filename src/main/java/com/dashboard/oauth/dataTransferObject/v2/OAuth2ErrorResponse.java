package com.dashboard.oauth.dataTransferObject.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuth2ErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}
