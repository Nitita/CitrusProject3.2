package autotests;

import com.consol.citrus.TestActionRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.message.builder.ObjectMappingPayloadBuilder;
import com.consol.citrus.validation.json.JsonMappingValidationProcessor;
import static com.consol.citrus.dsl.MessageSupport.MessageHeaderSupport.fromHeaders;
import static com.consol.citrus.dsl.MessageSupport.MessageBodySupport.fromBody;
//import static com.consol.citrus.dsl.JsonPathSupport.jsonPath;
import model.Order;
import model.OrderRes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import java.util.Map;

import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static com.consol.citrus.actions.EchoAction.Builder.echo;
import static com.consol.citrus.validation.json.JsonPathMessageValidationContext.Builder.jsonPath;
import static org.hamcrest.Matchers.containsString;

public class Store2Tests extends BaseTest {

    String pathRequest = "/v2/store/order";

    @Test
    @CitrusTest
    public void testOrder(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .petId(123)
                .quantity(456)
                .shipDate("2023-02-10T00:00:00.000+0000")
                .status("work")
                .complete(true);

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(pathRequest)
                .message()
                .name("OrderReq")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new ObjectMappingPayloadBuilder(userData, objectMapper)));

        actions.$(http()
                .client(PetStoreClient)
                .receive()
                .response(HttpStatus.OK)
                .message()
                .name("OrderRes")
                .type(MessageType.JSON)
                .validate(new JsonMappingValidationProcessor<OrderRes>(OrderRes.class, objectMapper) {
                    @Override
                    public void validate(OrderRes todoEntry, Map<String, Object> headers, TestContext context) {
                        Assert.assertNotNull(todoEntry);
                        Assert.assertEquals(todoEntry.quantity(), 456);
                    }
                })
        );

        actions.$(echo("citrus:jsonPath(citrus:message(OrderRes), '$.shipDate')"));
    }

    @Test
    @CitrusTest
    public void testOrder1(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .petId(123)
                .quantity(456)
                .shipDate("2023-02-10T00:00:00.000+0000")
                .status("work")
                .complete(true);

        actions.$(http()
                .client(PetStoreClient)
                .send()
                .post(pathRequest)
                .message()
                .name("OrderReq")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new ObjectMappingPayloadBuilder(userData, objectMapper)));

        actions.$(http()
                        .client(PetStoreClient)
                        .receive()
                        .response(HttpStatus.OK)
                        .message()
                        .name("OrderRes")
                        .type(MessageType.JSON)
                .extract(fromHeaders()
                                .expression("Content-Type", "${authVar}"))
                .extract(fromBody()
                                .expression("$.quantity", "${quantityVar}"))
        );

        actions.$(echo("Переменная из заголовка  ${quantityVar}"));

        actions.$(echo("Переменная из заголовка  ${authVar}"));
    }

    @Test
    @CitrusTest
    public void testOrder2(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .quantity(456)
                .shipDate("2023-02-10");

        sendPostRequest(actions, pathRequest, userData);

        validateResponse(actions, jsonPath()
                .expression("$.id", "1")
                .expression("$.quantity", "456")
                .expression("$.shipDate", containsString("2023-02-10")));
    }

    @Test
    @CitrusTest
    public void testOrder3(@Optional @CitrusResource TestActionRunner actions) {

        sendPostRequestWithBodyFromResource(actions, pathRequest, "autotests/storeTests/orderRequestBody.json");

        validateFullResponse(actions, "autotests/storeTests/orderResponseBody.json");
    }
}
