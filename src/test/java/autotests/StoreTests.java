package autotests;

import com.consol.citrus.TestActionRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.message.builder.ObjectMappingPayloadBuilder;
import model.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static com.consol.citrus.validation.json.JsonPathMessageValidationContext.Builder.jsonPath;

public class StoreTests extends BaseTest {

    String pathRequest = "/v2/store/order";

    @Test(invocationCount = 1)
    @CitrusTest
    public void testOrder1(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .petId(123)
                .quantity(456)
                .shipDate("2023-02-10")
                .status("work")
                .complete(true);

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(pathRequest)
                .message()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new ObjectMappingPayloadBuilder(userData, objectMapper)));

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .type(MessageType.JSON)
                .validate(jsonPath()
                        .expression("$.petId", "1")
                        .expression("$.petId", "123")
                        .expression("$.quantity", "456")
                        .expression("$.shipDate", "2023-02-10T00:00:00.000+0000")
                        .expression("$.status", "work")
                        .expression("$.complete", "true")));
    }

    @Test
    @CitrusTest
    public void testOrder2(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .petId(123)
                .quantity(456)
                .shipDate("2023-02-10")
                .status("work")
                .complete(true);

        sendPostRequest(actions, pathRequest, userData);

        validateResponse(actions, jsonPath()
                .expression("$.id", "1")
                .expression("$.petId", "123")
                .expression("$.quantity", "456")
                .expression("$.shipDate", "@startsWith('2023-02-10')@")
                .expression("$.status", "work")
                .expression("$.complete", "true"));

    }

    @Test
    @CitrusTest
    public void testOrder3(@Optional @CitrusResource TestActionRunner actions) {

        sendPostRequestWithBodyFromResource(actions, pathRequest, "autotests/storeTests/orderRequestBody.json");

        validateFullResponse(actions, "autotests/storeTests/orderResponseBody.json");
    }
}
