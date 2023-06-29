package test.java.gatlingdemostore;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

public class DemostoreSimulation extends Simulation {

    private static final String CART_TOTAL_LABEL = "cartTotal";
    private static final String DOMAIN = "demostore-1.sandbox.gatling.io";
    private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("http://" + DOMAIN);
    FeederBuilder.Batchable<String> searchFeeder = csv("data/search.csv").random();


    ChainBuilder homePage = exec(http("Load Home Page")
            .get("/")
            .check(css("#_csrf", "content").saveAs("csrfValue")));
    ChainBuilder categoriesPage = exec(http("Load Categories Page").get("/category/all"));
    ChainBuilder viewRandomProduct = feed(searchFeeder).exec(http("Load Product Page : #{path}").get("/product/#{path}"));
    ChainBuilder addRandomProduct = viewRandomProduct
            .exec(http("Add product to cart : #{path}").get("/cart/add/#{id}"))
            .exec(session -> {
                Double currentCartTotal = session.getDouble(CART_TOTAL_LABEL);
                Double itemPrice = session.getDouble("price");
                return session.set(CART_TOTAL_LABEL, currentCartTotal + itemPrice);
            });
    ChainBuilder viewCart = exec(http("View cart")
            .get("/cart/view")
            .check(css("#grandTotal").isEL("$#{" + CART_TOTAL_LABEL + "}")));
    ChainBuilder checkoutCart = exec(http("Checkout Cart").get("/cart/checkout"));

    ChainBuilder login = exec(http("Login page").get("/login"))
            .exec(http("Login ").post("/login")
                    .formParam("_csrf", "#{csrfValue}")
                    .formParam("username", "user1")
                    .formParam("password", "pass")
            );


    ChainBuilder purchaser =
            exec(session -> session.set(CART_TOTAL_LABEL, 0.00))
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct)
                    .exec(login)
                    .exec(viewCart)
                    .exec(checkoutCart);

    ChainBuilder abandoner =
            exec(session -> session.set(CART_TOTAL_LABEL, 0.00))
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct);

    ChainBuilder browser =
            exec(session -> session.set(CART_TOTAL_LABEL, 0.00))
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct));
    ScenarioBuilder scn =
            scenario("Default scenario")
                    .during(Duration.ofSeconds(10))
                    .on(
                            randomSwitch().on(
                                    Choice.withWeight(75.00, exec(browser)),
                                    Choice.withWeight(15.00, exec(abandoner)),
                                    Choice.withWeight(10.00, exec(purchaser))
                            )
                    );

    {
        setUp(scn.injectOpen(constantUsersPerSec(40).during(50))).protocols(HTTP_PROTOCOL)
                .assertions(
                        global().responseTime().percentile4().lt(1000)
                );
    }
}