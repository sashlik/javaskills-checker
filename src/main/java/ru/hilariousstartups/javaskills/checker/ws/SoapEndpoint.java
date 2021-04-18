package ru.hilariousstartups.javaskills.checker.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import ru.hilariousstartups.javaskills.checker.apitester.ApiCallContext;
import ru.hilariousstartups.javaskills.checker.apitester.CallRepository;
import ru.hilariousstartups.javaskills.checker.apitester.TestStrategy;
import ru.hilariousstartups.soap.gen.Gender;
import ru.hilariousstartups.soap.gen.GetUserRequest;
import ru.hilariousstartups.soap.gen.GetUserResponse;
import ru.hilariousstartups.soap.gen.User;

@Endpoint
@Slf4j
public class SoapEndpoint {

    private final CallRepository callRepository;

    public SoapEndpoint(CallRepository callRepository) {
        this.callRepository = callRepository;
    }

    private static final String NAMESPACE_URI = "http://hilariousstartups.ru/soap/gen";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getUserRequest")
    @ResponsePayload
    public GetUserResponse getUser(@RequestPayload GetUserRequest request) {

        ApiCallContext callContext = callRepository.get(request.getUserId());
        if (callContext == null) {
            throw new RuntimeException("User with id " + request.getUserId() + " not found");
        }

        if (callContext.getTestStrategy() == TestStrategy.WS_TIMEOUT) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {

            }
        }
        else if (callContext.getTestStrategy() == TestStrategy.WS_ERROR) {
            throw new RuntimeException("Internal server error");
        }
        else if (callContext.getTestStrategy() == TestStrategy.ASYNC) {
            callContext.waitOtherCalls();

        }

        GetUserResponse response = new GetUserResponse();
        User user = new User();

        user.setFirstName(callContext.getGeneratedUser().getFirstName());
        user.setLastName(callContext.getGeneratedUser().getLastName());
        user.setGender(Gender.MALE);
        response.setUser(user);
        return response;
    }

}
