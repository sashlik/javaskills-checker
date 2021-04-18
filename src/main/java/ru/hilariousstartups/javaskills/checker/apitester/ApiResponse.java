package ru.hilariousstartups.javaskills.checker.apitester;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {

    private Integer code; // 0 - ok, 1 - timeout, 2 - error
    private String name;
    private String phone;



}
