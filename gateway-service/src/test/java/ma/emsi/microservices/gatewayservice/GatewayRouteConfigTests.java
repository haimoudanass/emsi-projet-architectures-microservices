package ma.emsi.microservices.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRouteConfigTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsWithEurekaIsolated() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("gateway-service");
        assertThat(environment.getProperty("server.port")).isEqualTo("9999");
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.register-with-eureka", Boolean.class)).isFalse();
        assertThat(environment.getProperty("eureka.client.fetch-registry", Boolean.class)).isFalse();
    }

    @Test
    void productionYamlEnablesDiscoveryLocatorWithoutStaticRoutes() throws Exception {
        String yaml = new ClassPathResource("application.yml")
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(yaml).contains("discovery:");
        assertThat(yaml).contains("locator:");
        assertThat(yaml).contains("enabled: true");
        assertThat(yaml).contains("lower-case-service-id: false");
        assertThat(yaml).doesNotContain("customer-route");
        assertThat(yaml).doesNotContain("ebank-route");
        assertThat(yaml).doesNotContain("http://localhost:8056");
        assertThat(yaml).doesNotContain("http://localhost:8057");
        assertThat(yaml).contains("defaultZone: http://localhost:8761/eureka/");
    }

    @Test
    void noStaticHttpRoutesAreRegisteredInContext() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
        assertThat(routes).isNotNull();
        assertThat(routes).extracting(RouteDefinition::getId)
                .doesNotContain("customer-route", "ebank-route");
        assertThat(routes).noneMatch(route ->
                "http://localhost:8056".equals(route.getUri().toString())
                        || "http://localhost:8057".equals(route.getUri().toString()));
    }
}
