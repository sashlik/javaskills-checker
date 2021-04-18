package ru.hilariousstartups.javaskills.checker.apitester;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiCallResult {

    public static final ApiCallResult UNAUTHORIZED = new ApiCallResult(null, true, false, null);

    private ApiResponse apiResponse;
    private Boolean unauth;
    private Boolean error;
    private String description;

    public boolean ok() {
        return error == null || !error;
    }

    public static ApiCallResult error(String description) {
        return new ApiCallResult(null, false, true, description);
    }

    public static ApiCallResult ok(ApiResponse apiResponse) {
        return new ApiCallResult(apiResponse, false, false, null);
    }

}
