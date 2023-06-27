package test.java.gatlingdemostore;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class DemostoreSimulation extends Simulation {

    private static final String DOMAIN = "demostore.gatling.io";
    private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("http://" + DOMAIN);

    private final ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(
                            http("Load Home Page")
                                    .get("/")
                    );
    {
        setUp(scn.injectOpen(atOnceUsers(1))).protocols(HTTP_PROTOCOL);
    }
}
