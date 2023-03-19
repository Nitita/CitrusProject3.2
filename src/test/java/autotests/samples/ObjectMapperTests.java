package autotests.samples;

import autotests.BaseTest;
import com.consol.citrus.TestActionRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.message.builder.ObjectMappingPayloadBuilder;
import com.consol.citrus.validation.json.JsonMappingValidationProcessor;
import model.Order;
import model.OrderRes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import java.util.Map;

import static com.consol.citrus.actions.EchoAction.Builder.echo;
import static com.consol.citrus.dsl.MessageSupport.MessageBodySupport.fromBody;
import static com.consol.citrus.dsl.MessageSupport.MessageHeaderSupport.fromHeaders;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;

public class ObjectMapperTests extends BaseTest {

    String pathRequest = "/store/order";

    @Test(description = "описание теста")
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
                .validate(new JsonMappingValidationProcessor<OrderRes>(OrderRes.class, objectMapper) {
                    @Override
                    public void validate(OrderRes orderRes, Map<String, Object> headers, TestContext context) {
                        Assert.assertNotNull(orderRes);
                        Assert.assertEquals(orderRes.quantity(), 456);
                    }
                })
        );
    }

    @Test
    @CitrusTest
    public void testOrder2(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .petId(123)
                .quantity(456);

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
                .extract(fromHeaders()
                        .expression("Content-Type", "${authVar}"))
                .extract(fromBody()
                        .expression("$.quantity", "${quantityVar}"))
        );

        actions.$(echo("Content-Type -->  ${quantityVar}"));

        actions.$(echo("quantity -->  ${authVar}"));
    }

    @Test
    @CitrusTest
    public void testOrder3(@Optional @CitrusResource TestActionRunner actions) {

        Order userData = new Order()
                .id(1)
                .shipDate("2023-02-10")
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
                .name("OrderRes"));

        actions.$(echo("citrus:jsonPath(citrus:message(OrderRes), '$.shipDate')"));
    }
}
