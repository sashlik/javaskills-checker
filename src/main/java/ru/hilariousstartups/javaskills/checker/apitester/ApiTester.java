package ru.hilariousstartups.javaskills.checker.apitester;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
@Slf4j
public class ApiTester implements ApplicationListener<ApplicationReadyEvent> {

    private final ApplicationContext appContext;
    private final Generator generator;
    private final CallRepository callRepository;
    private final String apiEndpoint;
    private final Integer highloadClients;
    private final Integer highloadCallPerClient;

    public ApiTester(@Value("${api.endpoint}") String apiEndpoint,
                     @Value("${highload.clients:5}") Integer highloadClients,
                     @Value("${highload.callPerClient:10}") Integer highloadCallPerClient,
                     ApplicationContext appContext, Generator generator, CallRepository callRepository) {
        this.apiEndpoint = apiEndpoint;
        this.appContext = appContext;
        this.generator = generator;
        this.callRepository = callRepository;
        this.highloadClients = highloadClients;
        this.highloadCallPerClient = highloadCallPerClient;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        String authTestDescr = "Проверка авторизации";

        ApiCallResult authTest1 = callApi(generator.generateAndStore(), false);
        ApiCallResult authTest2 = callApi(generator.generateAndStore(), true);

       if (!authTest1.ok()) {
            System.out.print(authTestDescr + " FAILED: " + authTest1.getDescription());
            terminate();
            return;
        } else if (!authTest2.ok()) {
            System.out.print(authTestDescr + " FAILED: " + authTest2.getDescription());
            terminate();
            return;
        } else {
            System.out.println(authTestDescr + " OK!");
        }

        if (!test("Проверка результата работы API", null)) {
            terminate();
            return;
        }


        if (!test("Тест на таймаут веб-сервиса", TestStrategy.WS_TIMEOUT)) {
            terminate();
            return;
        }

        if (!test("Тест на таймаут REST сервиса", TestStrategy.RS_TIMEOUT)) {
            terminate();
            return;
        }

        if (!test("Тест на ошибку от веб-сервиса", TestStrategy.WS_ERROR)) {
            terminate();
            return;
        }

        if (!test("Тест на ошибку от REST сервиса", TestStrategy.RS_ERROR)) {
            terminate();
            return;
        }
        if (!test("Тест на параллельность вызовов сервисов", TestStrategy.ASYNC)) {
            terminate();
            return;
        }



        Thread[] callers = new Thread[highloadClients];
        FutureTask<Boolean>[] tasks = new FutureTask[highloadClients];
        AtomicInteger successCounter = new AtomicInteger(0);


        IntStream.range(0, tasks.length).forEach(i -> {
            tasks[i] = new FutureTask<Boolean>(() -> {
                IntStream.range(0, highloadCallPerClient).forEach(j -> {
                    if (test("Нагрузочный тест", TestStrategy.HIGHLOAD)) {
                        successCounter.incrementAndGet();
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {}
                    }
                });
                return true;

            });
            callers[i] = new Thread(tasks[i]);
            callers[i].start();
        });


        System.out.println("Нагрузочный тест");
        try {
            Thread.sleep(highloadCallPerClient  * 1000); // 1 sec per call + 500 millis between calls
            int success = successCounter.get();
            int total = highloadClients * highloadCallPerClient;
            String result = success == 0 ? "FAILED" :
                              success ==  total ? "OK" : "UNSTABLE";
            System.out.println(" " + result + " " + success + " из " + total);
        } catch (InterruptedException e) {
            System.out.println(" FAILED " + e.getMessage());
        }

        System.out.println("Все проверки пройдены OK!");
        terminate();
    }

    private boolean test(String testDescription, TestStrategy testStrategy) {
        if (TestStrategy.HIGHLOAD != testStrategy) System.out.print(testDescription);
        Integer id = generator.generateAndStore(testStrategy);
        ApiCallResult apiCallResult = callApi(id);
        if (!apiCallResult.ok()) {
            if (TestStrategy.HIGHLOAD != testStrategy) System.out.println(" FAILED: " + apiCallResult.getDescription());
            return false;
        } else {
            ApiCallContext callContext = callRepository.get(id);
            callContext.setResult(apiCallResult.getApiResponse());
            if (testStrategy == TestStrategy.ASYNC) {
                if (callContext.isConfirmed()) {
                    if (TestStrategy.HIGHLOAD != testStrategy) System.out.println(" OK!");
                    return true;
                } else {
                    if (TestStrategy.HIGHLOAD != testStrategy) System.out.println(" FAILED\n Запросы необходимо отправлять параллельно");
                    return false;
                }
            }


            if (callContext.resultMatch()) {
                if (TestStrategy.HIGHLOAD != testStrategy) System.out.println(" OK!");
                return true;
            } else {
                if (TestStrategy.HIGHLOAD != testStrategy) System.out.println(" FAILED\nОжидалось " + callContext.expected() + "\nПришло " + callContext.getResult());
                return false;
            }
        }
    }

    private void terminate() {
        SpringApplication.exit(appContext, () -> 0);
    }

    private ApiCallResult callApi(Integer id) {
        return callApi(id, true);
    }

    private ApiCallResult callApi(Integer id, boolean withCredentials) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            if (withCredentials) {
                headers.setBasicAuth("testuser", "password123");
            }


            ResponseEntity<ApiResponse> response = restTemplate.exchange(apiEndpoint + "/user/" + id, HttpMethod.GET, entity, ApiResponse.class);
            if (!withCredentials) {
                return ApiCallResult.error("Отсутствует авторизация");
            }

            return ApiCallResult.ok(response.getBody());

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                if (e.getResponseHeaders() != null) {
                    List<String> www_authenticate = e.getResponseHeaders().get(HttpHeaders.WWW_AUTHENTICATE);
                    if (www_authenticate != null) {
                        String basic = www_authenticate.stream().filter(h -> h.startsWith("Basic")).findAny().orElse(null);
                        if (basic != null) {
                            return ApiCallResult.UNAUTHORIZED;
                        }
                    }
                }

                return ApiCallResult.error("Проблема с авторизацией. Код " + HttpStatus.UNAUTHORIZED + ", но не указана схема авторизации");
            } else {
                return ApiCallResult.error("Код ответа " + e.getStatusCode() + ", ожидался 200");

            }
        } catch (RestClientException e) {
            return ApiCallResult.error("Ошибка вызова API: " + e.getMessage());
        } catch (Exception e) {
            return ApiCallResult.error("Ошибка " + e.getMessage());
        }
    }
}
