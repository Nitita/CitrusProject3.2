package autotests.samples;

import autotests.BaseTest;
import com.consol.citrus.TestActionRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import model.Order;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import static com.consol.citrus.validation.json.JsonPathMessageValidationContext.Builder.jsonPath;

public class FinishTests extends BaseTest {

    String pathRequest = "/store/order";

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
