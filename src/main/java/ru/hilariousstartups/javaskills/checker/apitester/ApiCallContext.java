package ru.hilariousstartups.javaskills.checker.apitester;

import lombok.Builder;
import lombok.Data;
import ru.hilariousstartups.soap.gen.User;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Data
@Builder
public class ApiCallContext {


    private ApiResponse result;
    private User generatedUser;
    private List<String> generatedPhones;
    private TestStrategy testStrategy;
    private AtomicInteger counter;
    private boolean confirmed;


    public boolean resultMatch() {
        return  expected().equals(result);
    }

    public ApiResponse expected() {

        String name = generatedUser.getFirstName() + " " + generatedUser.getLastName();
        String phone = generatedPhones.get(0);

        if (testStrategy == null) {
            return new ApiResponse(0, name, phone);
        }

        switch (testStrategy) {
            case WS_TIMEOUT:
                return new ApiResponse(1, null, null);
            case RS_TIMEOUT:
            case RS_ERROR:
                return new ApiResponse(0, name, null);
            case WS_ERROR:
                return new ApiResponse(2, null, null);
            default:
                return new ApiResponse(0, name, phone);
        }
    }



    public void waitOtherCalls() {
        int cnt = counter.incrementAndGet();
        if (cnt == 1) {
            IntStream.range(0, 10).forEach(i -> {
                if (counter.get() >= 2) {
                    confirmed = true;
                }
                else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}
                }
            });
        }
    }
}
