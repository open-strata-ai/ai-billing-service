package cc.openstrata.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BillingServiceApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context wires (H2-backend, in-memory adapters) offline.
    }
}
