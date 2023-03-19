package autotests.samples;

import autotests.BaseTest;
import com.consol.citrus.TestActionRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.message.MessageType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static com.consol.citrus.validation.json.JsonPathMessageValidationContext.Builder.jsonPath;

public class FirstTests extends BaseTest {

    String pathRequest = "/store/order";


    @Test(description = "описание теста")
    @CitrusTest
    public void testOrder1(@Optional @CitrusResource TestActionRunner actions) {

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(pathRequest)
                .message()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\n" +
                        "  \"petId\": 123,\n" +
                        "  \"quantity\": 456,\n" +
                        "  \"id\": 1,\n" +
                        "  \"shipDate\": \"2023-02-10\",\n" +
                        "  \"complete\": true,\n" +
                        "  \"status\": \"work\"\n" +
                        "}"));

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .type(MessageType.JSON)
                .body("{\n" +
                        "  \"id\": 1,\n" +
                        "  \"petId\": 123,\n" +
                        "  \"quantity\": 456,\n" +
                        "  \"shipDate\": \"@startsWith('2023-02-10')@\",\n" +
                        "  \"status\": \"work\",\n" +
                        "  \"complete\": true\n" +
                        "}"));
    }


    @Test(description = "описание теста")
    @CitrusTest
    public void testOrder2(@Optional @CitrusResource TestActionRunner actions) {

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(pathRequest)
                .message()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\n" +
                        "  \"petId\": 123,\n" +
                        "  \"quantity\": 456,\n" +
                        "  \"id\": 1,\n" +
                        "  \"shipDate\": \"2023-02-10\",\n" +
                        "  \"complete\": true,\n" +
                        "  \"status\": \"work\"\n" +
                        "}"));

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .type(MessageType.JSON)
                .validate(jsonPath()
                        .expression("id", "1")
                        .expression("petId", "123")
                        .expression("quantity", "456")
                        .expression("shipDate", "@startsWith('2023-02-10')@")
                        .expression("status", "work")
                        .expression("complete", "true")));
    }


    @Test(description = "описание теста")
    @CitrusTest
    public void testOrder3(@Optional @CitrusResource TestActionRunner actions) {

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(pathRequest)
                .message()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new ClassPathResource("autotests/storeTests/orderRequestBody.json")));

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .type(MessageType.JSON)
                .body(new ClassPathResource("autotests/storeTests/orderResponseBody.json")));
    }

}
