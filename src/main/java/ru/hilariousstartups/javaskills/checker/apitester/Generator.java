package ru.hilariousstartups.javaskills.checker.apitester;

import org.springframework.stereotype.Component;
import ru.hilariousstartups.soap.gen.Gender;
import ru.hilariousstartups.soap.gen.User;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Generator {

    private final CallRepository callRepository;

    public Generator(CallRepository callRepository) {
        this.callRepository = callRepository;
    }

    private List<String> names = List.of("Александр", "Петр", "Олег", "Иван", "Даниил", "Николай", "Василий", "Роман", "Игорь");
    private List<String> lastNames = List.of("Морозов", "Соколов", "Щукин", "Капустин", "Осипов", "Лапин", "Рыбаков", "Зайцев", "Голубев");

    public Integer generateAndStore() {
        return generateAndStore(null);
    }

    public Integer generateAndStore(TestStrategy testStrategy) {
        User generatedUser = new User();
        generatedUser.setLastName(generateLastName());
        generatedUser.setFirstName(generateFirstName());
        generatedUser.setGender(Gender.MALE);

        List<String> phones = List.of(generatePhone(), generatePhone(), generatePhone());

        ApiCallContext callContext = ApiCallContext.builder().generatedUser(generatedUser).generatedPhones(phones).testStrategy(testStrategy).counter(new AtomicInteger(0)).build();
        Integer id = rndId();

        callRepository.put(id, callContext);
        return id;
    }

    private String generateFirstName() {
        return names.get(ThreadLocalRandom.current().nextInt(names.size()));
    }

    private String generateLastName() {
        return lastNames.get(ThreadLocalRandom.current().nextInt(lastNames.size()));
    }

    private String generatePhone() {
       return "+7 (495) " + (ThreadLocalRandom.current().nextInt(899) + 100) + "-" +
               (ThreadLocalRandom.current().nextInt(89) + 10) + "-" +
               (ThreadLocalRandom.current().nextInt(89) + 10);
    }

    private Integer rndId() {
        int id;
        do {
            id = ThreadLocalRandom.current().nextInt(100000);
        }
        while (callRepository.get(id) != null);

        return id;
    }

}
