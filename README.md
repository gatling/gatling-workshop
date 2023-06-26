Hello fellow testers, welcome to our Gatling Workshop.

Here is what's you might expect to learn shall you decide to stick with us during the approximately an hour and a half : 

- Design a first Hello World simulation
- Run it on the cloud.
- How to use useful tools of our DSL, that will allow you to closely simulate user's behaviour, perform automated checks, save variables etc. 
- Analyse graphs


First of all, here is the Hello world simulation I was mentioning earlier : 

```java 
public class DemostoreSimulation extends Simulation {

    private static final String DOMAIN = "demostore.gatling.io";                                    // 1
    private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("https://" + DOMAIN);     // 2

    private static final ScenarioBuilder scn =
            scenario("Browse glasses")                                                              // 3
                    .exec(
                            http("Load Home Page")                                                  // 4
                                    .get("/")                                                       // 5
                    );

    {
        setUp(scn.injectOpen(atOnceUsers(1))).protocols(HTTP_PROTOCOL);                             // 6 
    }
}
```
- 1 : The domain name of the application under test. 
- 2 : The base URL, referred to as HTTP_PROTOCOL
- 3 : The name of your scenario as it will appear in the reports.
- 4 : The name of the request as it will appear on the reports
- 5 : The METHOD and PATH of the request
- 6 : The injection profile. Here we define a smoke test, we shoot once 1 user. 


Now, usually when users browse glasses, they do not simply load the home page. Let's make them browse through the "all" category and click on a few items.
Of course they are not robots, so we'll also add artificial pauses, to make it more realistic. 

### Chain requests

```java 
            scenario("Browse glasses")
                    .exec(
                            http("Load Home Page")
                                    .get("/")
                    )
                    .pause(2)
                    .exec(http("Load Categories Page").get("/category/all"))
                    .pause(2)
                    .exec(http("Load Product Page").get("/product/black-and-red-glasses"))
                    .pause(2)
                    .exec(http("Load Categories Page").get("/category/all"))
                    .pause(2)
                    .exec(http("Load Product Page").get("/product/deepest-blue"))
```


Nice ! It's starting to look like an actual user workflow. But we still have a problem. Do you see it coming ? ...  
If we were to simulation thousands of users, it would be highly unlikely for them to browse exactly the same items. Let's randomize that a bit shall we ?


### Feeders 

So we'll create a CSV feeder from which each simulated user will randomly choose a particular type of glasses to browse.
So let's add a data/search.csv file containing : 

```csv 
path, id, price
deepest-blue, 22, 29.99
casual-black-blue, 17, 24.99
black-and-red-glasses, 19, 18.99
bright-yellow-glasses, 20, 17.99
casual-brown-glasses, 21, 13.99
light-blue-glasses, 23, 17.99
sky-blue-case, 24, 8.99
white-casual-case, 25, 16.99
perfect-pink, 26, 19.99
curved-black, 27, 12.99
black-grey-curved, 28, 14.99
black-light-blue, 29, 23.99
curved-pink, 30, 16.99
velvet-red, 31, 18.99
deep-blue-ocean, 32, 27.99
curved-brown, 33, 19.99
leopard-skin, 34, 22.99
gold-design, 35, 39.99
pink-panther, 36, 27.99
curve-ocean-sky, 37, 18.99
plain-white, 38, 9.99
white-leopard-pattern, 39, 13.99
```

Now we retrieve this file as a feeder in our simulation : 

```java 
    FeederBuilder.Batchable<String> searchFeeder = csv("data/search.csv").random();
```

And then use it to inject values in our requests : 

```java 
.pause(2)
.feed(searchFeeder)
        .exec(http("Load Product Page : #{path}").get("/product/#{path}"))
```

Ok cool, now it's randomized ! We have a first coherent visitor journey. 
Let's tidy up things before we go on with more complex flows. 

First we can divide our scenario into request chains. 
Let's extract each request into its dedicated chain : 

```java
    ChainBuilder homePage = exec(http("Load Home Page").get("/"));
    ChainBuilder categoriesPage = exec(http("Load Categories Page").get("/category/all"));
    ChainBuilder viewRandomProduct = feed(searchFeeder).exec(http("Load Product Page : #{path}").get("/product/#{path}"));
```

And then use them in our scenario : 

```java 
ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .pause(2)
                    .exec(viewRandomProduct)
                    .pause(2)
                    .exec(viewRandomProduct);
```

Now we see that we have a repetition. You don't like that. I don't like that. Let's change it with `repeat` keyword : 

```java 
.repeat(2).on(pause(2).exec(viewRandomProduct))
```

Let's add an item to the cart. 

```java 
    ChainBuilder addRandomProduct = viewRandomProduct.exec(http("Add product to cart : #{path}").get("/cart/add/#{id}"));
```

```java 
    ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct);
```

And then view cart : 

```java
    ChainBuilder viewCart = exec(http("View cart").get("/cart/view"));
```
```java 
    ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct)
                    .exec(viewCart);
```

Ok now our users have an item in their carts, but they can't checkout as they are not logged in. Let's log them in. 

```java 
    ChainBuilder login = exec(http("Login page").get("/login"))
            .exec(http("Login").post("login")
                    .formParam("username", "user1")
                    .formParam("password", "pass")
            );
```

```java 
    ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct)
                    .exec(viewCart)
                    .exec(login);
```

And that is not functional, we get an error on the login attempt. Why will you ask me ? 
Because Spring Security protects forms from csrf attacks by expecting a CSRF token that we did not provide. 
But in order to add it in the request, we must first retrieve it from the home page, and that is a great way to learn variabilisation in gatling.  
Damn this transition is so neat it looks like it was all part of a master plan. 

The `check` keywork in gatling DSL allows for many things related to reading the response we get from the request. 
One can perform actual checks, like verifying the presence of a string, or a return value. 
But it is also useful to save response elements in a variable that will be accessible later on. That is what we are going to do to retrieve the _csrf token.

```java 
    ChainBuilder homePage = exec(http("Load Home Page")
            .get("/")
            .check(css("#_csrf", "content").saveAs("csrfValue")));
```

And then we can simply call this variable in our login form : 

```java 
    ChainBuilder login = exec(http("Login page").get("/login"))
            .exec(http("Login").post("login")
                    .formParam("_csrf", "#{csrfValue}")
                    .formParam("username", "user1")
                    .formParam("password", "pass")
            );
```

Now it works, we can proceed to checkout : 

```java 
    ChainBuilder checkoutCart =  exec(http("Checkout Cart").get("/cart/checkout"));
```

```java
    ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct)
                    .exec(viewCart)
                    .exec(login)
                    .exec(checkoutCart);
```

Ok, that is quite nice, but we would like to verify the cart total for each user. That is not necessarily linked to the app's performance (though it might), but we also like functional testing. (And it helps me with my transitions).
That means that we need to remember the current cart total price for each user. 
Unfortunately, the `check` keyword won't help here, because it's global. 
We need to introduce a new concept : the Session

One of the first benefits of using a session, is that we can store user local variables. 
Let's initialize the expected cart total to 0.00 at the beginning : 

```java 
            scenario("Browse glasses")
                    .exec(session -> session.set(CART_TOTAL_LABEL, 0.00))
```

Then we want to keep track of this total each time a new item is added to the cart : 

```java 
    ChainBuilder addRandomProduct = viewRandomProduct
            .exec(http("Add product to cart : #{path}").get("/cart/add/#{id}"))
            .exec(session -> {
                Double currentCartTotal = session.getDouble(CART_TOTAL_LABEL);
                Double itemPrice = session.getDouble("price");
                return session.set(CART_TOTAL_LABEL, currentCartTotal + itemPrice);
            });
```

Here, you might notice that we never set the "price" variable, but since it is accessible from the CSV feeder, we can access it here as well.


Finally, we want to `check` that each time we go to view cart, the printed cart total equals the one that we calculated. 
As you might have figured out thanks to my subtle nudge, we will use the `check` keyword for that.

But first we need to know where to read it from. So let's manually go to the website, open dev tools and find out where in the html page is located this total.
I did it for you, it's located under the id `grandTotal` but I tend to lie sometimes, you might as well check it yourself. 

So now we have everything we need to perform that automated check : 

```java
    ChainBuilder viewCart = exec(http("View cart")
            .get("/cart/view")
            .check(css("#grandTotal").isEL("$#{"+CART_TOTAL_LABEL+"}")));
```

```java 
    ScenarioBuilder scn =
            scenario("Browse glasses")
                    .exec(session -> session.set(CART_TOTAL_LABEL, 0.00))
                    .exec(homePage)
                    .pause(2)
                    .exec(categoriesPage)
                    .repeat(2).on(pause(2).exec(viewRandomProduct))
                    .exec(addRandomProduct)
                    .exec(login)
                    .exec(viewCart)
                    .exec(checkoutCart);
```

You'll notice that I inverted the login and the view cart requests. That is because if users are not already logged in when accessing their cart, they are redirected to the login page, where obviously the `#grandTotal`cannot be found. 


OK nice, now we got ourselves a nice purchase flow. But all our users are expected to purchase? Unfortunately not. 
If we want to be realistic, we need to model that as well in our tests. 

First, let's define 3 different flows : 
- The purchaser, the one we already have;
- The browser, they are only taking a look at the catalog.
- The abandoner, they fill a cart but never checkout. 

It's quite easy since we already factorized requests. We only need to reassemble them in 3 different ways. 

```java
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
```

We have 3 different flows, and we want our scenario to go through them kind of randomly, but with a specified distribution.
We can expect that a vast majority of users will be browsers, and then almost as much purchasers than abandoners.

A way to define that would be : 

```java 
    ScenarioBuilder scn =
            scenario("Default scenario")
                    .during(Duration.ofSeconds(20))
                    .on(
                            randomSwitch().on(
                                    Choice.withWeight(75.00, exec(browser)),
                                    Choice.withWeight(15.00, exec(abandoner)),
                                    Choice.withWeight(10.00, exec(purchaser))
                            )
                    );
```

But the results we get are not interesting, since from the beginning we're only playing with one only user.
It is time to set up a proper **injection profile**

A few things to know first.
We use 2 types of injection profiles to reproduce actual production use: Open and Closed
- Open means everyone can access your api, start a connection at any time. And that the servers will split their resources among all users. This is most probably what you need (99%).
- Closed is used to simulate a queue in front of your api, like for some ticketing websites, where the number of concurrent users connected to your api is capped.

We will dig into the open models here. It is possible to combine different injections to build more complex ones. But the bascis are : 
- Soak test : Tests how your application behaves over time with a reasonable and constant load.
- Capacity test : We progressively increase the number of users arriving to your website, to see when performances start decreasing.?
- Stress test : We quickly inject a high peak of users and then go back to normal, to see how your app crashes and recovers. 

Ok
now that I said that, let's first perform a soak test : 

