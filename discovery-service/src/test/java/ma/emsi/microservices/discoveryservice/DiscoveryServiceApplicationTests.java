package ma.emsi.microservices.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DiscoveryServiceApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsAsEurekaServer() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("discovery-service");
        assertThat(environment.getProperty("server.port")).isEqualTo("8761");
        assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
    }
}
