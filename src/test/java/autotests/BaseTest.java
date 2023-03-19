package autotests;

import com.consol.citrus.TestActionRunner;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.message.builder.ObjectMappingPayloadBuilder;
import com.consol.citrus.testng.spring.TestNGCitrusSpringSupport;
import com.consol.citrus.validation.json.JsonPathMessageValidationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import static com.consol.citrus.http.actions.HttpActionBuilder.http;

@ContextConfiguration(classes = {EndpointConfig.class, AppConfig.class})

public class BaseTest extends TestNGCitrusSpringSupport {

    @Autowired
    protected HttpClient PetStoreClient;

    @Autowired
    protected ObjectMapper objectMapper;


    //Функция для отправки POST запросов с телом json сформированным в тесте динамически
    public void sendPostRequest(TestActionRunner actions, String path, Object userData) {

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(path)
                .message()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new ObjectMappingPayloadBuilder(userData, objectMapper)));
    }

    //Функция для отправки запросов с телом json из файла ресурсов по указанному пути
    public void sendPostRequestWithBodyFromResource(TestActionRunner actions, String path, String body) {

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(path)
                .message()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new ClassPathResource(body)));
    }


    //Функция валидации ответ по парметру (ключ - значение)
    public void validateResponse(TestActionRunner actions, JsonPathMessageValidationContext.Builder body) {

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .type(MessageType.JSON)
                .validate(body));
    }

    //Функция валидации всего json ответ с имеющимся в файле по указанному пути
    public void validateFullResponse(TestActionRunner actions, String body) {

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .type(MessageType.JSON)
                .body(new ClassPathResource(body)));
    }

}
