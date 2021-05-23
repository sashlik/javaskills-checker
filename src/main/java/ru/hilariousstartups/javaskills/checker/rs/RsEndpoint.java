package ru.hilariousstartups.javaskills.checker.rs;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hilariousstartups.javaskills.checker.apitester.ApiCallContext;
import ru.hilariousstartups.javaskills.checker.apitester.CallRepository;
import ru.hilariousstartups.javaskills.checker.apitester.TestStrategy;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class RsEndpoint {

    private final CallRepository callRepository;

    public RsEndpoint(CallRepository callRepository) {
        this.callRepository = callRepository;
    }

    @GetMapping("/phones/{id}")
    public ResponseEntity<PhonesResponse> phones(@PathVariable("id") final Integer id) {
        ApiCallContext callContext = callRepository.get(id);
        if (callContext == null) {
            return ResponseEntity.notFound().build();
        }

        if (callContext.getTestStrategy() == TestStrategy.RS_TIMEOUT) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {

            }
        }
        else if (callContext.getTestStrategy() == TestStrategy.RS_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        else if (callContext.getTestStrategy() == TestStrategy.ASYNC) {
            callContext.waitOtherCalls();
        }

        PhonesResponse phonesResponse = new PhonesResponse();
        phonesResponse.setPhones(callContext.getGeneratedPhones());
        return ResponseEntity.ok(phonesResponse);
    }

}
