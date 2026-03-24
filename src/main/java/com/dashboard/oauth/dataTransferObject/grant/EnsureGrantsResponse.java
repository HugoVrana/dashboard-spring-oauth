package com.dashboard.oauth.dataTransferObject.grant;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EnsureGrantsResponse {
    private List<String> created;
    private List<String> alreadyExisted;
}
