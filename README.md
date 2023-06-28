### Hello fellow testers, welcome to our Gatling Workshop.

# Introduction

Here is what's you might expect to learn shall you decide to stick with us during the approximately an hour and a half : 

- Design a first Hello World simulation
- Run it on the cloud.
- How to use useful tools of our DSL, that will allow you to closely simulate user's behaviour, perform automated checks, save variables etc. 
- Analyse graphs

## Au menu
1. [Prerequisites](#prerequisites)
2. [First run](#first-run)
3. [Explore the DSL](#explore-the-dsl)
4. [Chain requests](#chain-requests)
5. [Feeders](#feeders)
6. [Refactoring](#refactoring)
7. [Save global variables](#save-global-variables)
8. [User sessions](#user-sessions)
9. [Injection profiles](#injection-profiles)
10. [Assertions](#assertions)

# Prerequisites

Beforehand, I'll let you visit our [cloud](https://cloud.gatling.io/) and create your account there. It's entirely free and you'll get 60 minutes to start playing around. Which is more than enough to complete this workshop !  
Then, please ensure you have:
* Java 17 installed
* This repo cloned on your machine
* The project opened in your favorite IDE 

# First run

First of all, here is the Hello world simulation I was mentioning earlier : 

```java 
public class DemostoreSimulation extends Simulation {

    private static final String DOMAIN = "demostore-X.sandbox.gatling.io";                      // 1
    private static final HttpProtocolBuilder HTTP_PROTOCOL = http.baseUrl("http://" + DOMAIN); // 2

    private final ScenarioBuilder scn =
            scenario("Browse glasses")                                                          // 3
                    .exec(
                            http("Load Home Page")                                              // 4
                                    .get("/")                                                   // 5
                    );

    {
        setUp(scn.injectOpen(atOnceUsers(1))).protocols(HTTP_PROTOCOL);                         // 6 
    }
}
```
- 1 : The domain name of the application under test. 
- 2 : The base URL, referred to as HTTP_PROTOCOL
- 3 : The name of your scenario as it will appear in the reports.
- 4 : The name of the request as it will appear on the reports
- 5 : The METHOD and PATH of the request
- 6 : The injection profile. Here we define a smoke test, we shoot once 1 user, just to ensure the test in working fine.


> We could run this simulation on the cloud to familiarize with the interface. 
> But first, we need to package this simulation into a nice jar file. In order to do so we'll use the gatling maven plugin.  
> 
> Run the following command at the root of this project:
> ```bash 
> ./mvnw gatling:enterprisePackage
> ```
> 
> That just created a gatling readable package in the form of a jar file containing a bit of metadata to build a nice cloud experience.  
> 
> Nice, now we can go to our [cloud](https://cloud.gatling.io/), in the `Simulation` tab, then hit **Create**. 
> - Enter a name for the simulation, pick the default team and hit **Create new package** since you don't already have one available.  
> - Choose a name for your package, browse your laptop to the `gatling-workshop/target/gatling-demostore-3.9.5-shaded.jar` that has been created previously.
> - In the classname field, pick the only available simulation class, the one we will be working on.
> - Finally, select your favorite location for your load generators to be spawned. Just so you know, our demostores are deployed in Paris, might as well be close to them but not mandatory, if you feel Japanese today follow your instincts.
> - Hit **Save** to create your simulation. Now it is time to give it a first go, you can click on the start simulation button.
> 
> In about a minute or so, you'll get your graphs ready, in the meantime we can go on with the workshop !

# Explore the DSL

Now, usually when users browse our shop, they do not simply load the home page. Let's make them browse through the "all" category and click on a few items.
Of course, they are not robots, so we'll also add artificial pauses, to make it more realistic.

## Chain requests

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
> We would be interested in running it again, to see if it works, and how different requests are handled in our results page.   
> 
> But going to the interface to upload the package and start the simulation is a bit cumbersome, and as we are going to do it a bunch of time, I suggest we take a different approach: building and uploading the package to the cloud in one single CLI command.    
> In order to do so, we first need to create an **API token**.   
> On the **API Tokens** page, hit **Create**, give it a name and select the **Configure** Organization role, which will give it all the permissions we'll need to upload packages and start simulations from the CLI (No need to configure team roles, as you already have the highest role at the organization level).  
> Click **Save** to generate the token, copy and paste it in the `pom.xml` under the `gatling-maven-plugin` section like so :
> 
> ```xml
> 
> <plugin>
>     <groupId>io.gatling</groupId>
>     <artifactId>gatling-maven-plugin</artifactId>
>     <version>${gatling-maven-plugin.version}</version>
>     <configuration>
>         <apiToken>YOUR_TOKEN</apiToken>
>     </configuration>
> </plugin>
> ```
> We also need to configure maven to use the previously created package.   
> Go to the **Packages** page, on the right side of your package row, there is a **Copy package ID to clipboard** button.
> Then, in the `pom.xml` file, add it next to the `<apiToken>` configuration under `<packageId>`
> 
> Finally, you can now 
> ```bash 
> ./mvnw gatling:enterpriseUpload
> ```
> ```bash
> ./mvnw gatling:enterpriseStart
> ```
> And let the CLI guide you through the steps.   
> Once your simulation started, you should see it running on your simulations page!

## Feeders 

Nice! It's starting to look like an actual user workflow. But we still have a problem. Do you see it coming ? ...  
If we were to simulate thousands of users, it would be highly unlikely for them to browse exactly the same items. Let's randomize that a bit, shall we ?


So we'll create a CSV feeder from which each simulated user will randomly choose a particular type of glasses to browse.
So let's add a `src/test/resources/data/search.csv` file containing : 

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

> If you run again your simulation, you'll see in the result page that some requests are now labelled with the slug of the item that have been randomly selected.

Ok cool, now it's randomized ! We have a first coherent visitor journey. 
Let's tidy up things before we go on with more complex flows. 


## Refactoring 
    
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
> Let's run again the simulation to ensure everything is working as expected and that we see the new add product and view cart requests. 

Ok now our users have an item in their carts, but they can't checkout as they are not logged in. Let's log them in. 

## Save global variables

The store provides a `post` endpoint to log in. It also provides dummy users you can use, like `user1/pass`.

```java 
    ChainBuilder login = exec(http("Login page").get("/login"))
            .exec(http("Login").post("/login")
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
> Run again the simulation, in the `Requests and Responses per Second` graph you should see an error.

If you try and run the simulation as it is, you will see that is not functional, that we get an error on the login attempt. Why will you ask me ?  
Because the store's Spring Security layer protects forms from CSRF attacks by expecting a **CSRF token** that we did not provide. 
But in order to add it in the request, we must first retrieve it from the home page, and that is a great way to learn variabilization in Gatling.  
Damn this transition is so neat it looks like it was all part of a master plan. 

The `check` keyword in gatling DSL allows for many things related to reading the response we get from the request. 
One can perform actual checks, like verifying the presence of a string, or a return value. 
But it is also useful to save response elements in a variable that will be accessible later on. That is what we are going to do to retrieve the `_csrf` token.

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
> Let's run once more the simulation to ensure the previously witnessed error disappeared.

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

## User sessions

One of the first benefits of using a session, is that we can store user local variables. 
Let's initialize the expected cart total to 0.00 at the beginning : 

```java 
    private static final String CART_TOTAL_LABEL = "cartTotal";
```

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

> Let's run that simulation to see if our checks are working as expected. Failed checks will appear in the `Errors per Second` graph. 

OK nice, now we got ourselves a nice purchase flow. But all our users are expected to purchase? Unfortunately not. 
If we want to be realistic, we need to model that as well in our tests. 

## Injection profiles 

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
                    .during(Duration.ofSeconds(10))
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

```java 
 setUp(scn.injectOpen(constantUsersPerSec(10).during(20))).protocols(HTTP_PROTOCOL);
```

> After the test has run on the cloud we can take a quick tour on the results.   
> Notably the one I want to emphasize lies in the **Users** tab.   
> 
> Here, our open injection profile directly shapes the **Users Arrival Rate**  which shall reflect a **constantUsersPerSec**, the rest is determined by the duration of the flow of a single user.   
> This dependence is completely reversed when we use a closed model. 

> One can also take a look at the `Summary` tab where stats are presented by requests. A quick look at the count will show that proportions between flows were respected. 

Ok now let's progressively push a bit our app to see how it responds to higher loads. Let's make it a capacity test !

First approach, we could define a profile like so : 

```java 
       setUp(scn.injectOpen(rampUsersPerSec(0).to(40).during(20)).protocols(HTTP_PROTOCOL));
```

This will continuously ramp up the number of new users arriving on the website every second from 0 to 40. 
This is a good first approach, but it does not let time to the system to stabilize, so we are not sure that we would have the same results with a soak test for each value.

We tend to prefer to craft a **staircase** profile where we regularly increment the number of new users per second, but leave it steady for a few seconds.
This is entirely possible by combining multiple **regular DSL** profiles together with `map` and `flatmap`, but there is also an alternative  **meta DSL** : 

```java 
        setUp(scn.injectOpen(
                incrementUsersPerSec(15)
                        .times(5)
                        .eachLevelLasting(Duration.ofSeconds(10))
                        .separatedByRampsLasting(Duration.ofSeconds(10))
                        .startingFrom(10)
        ).protocols(HTTP_PROTOCOL));
```

> Once we run it, we can take a look in the results page at the **Response Time Percentiles** graph for instance.    
> From there, depending on the level of performance required we can pin (by right clicking) on the graph the moment where it started being unacceptable.   
> It could be for instance that we want at least 99% of our users to experience a latency below 1 second. Which happened when the 99th percentile reached 1 second.   
> Then, on the **Users** tab, we can see how many concurrent users were on the site at that exact moment, which gives us a good approximation of the capacity of our servers.

Now we know approximately how high in capacity our infrastructure can go as of today. Let's make sure that we won't introduce any regressions in our next deployments.
Let's integrate load testing in our CI pipeline !

### Assertions

It's fairly easy to integrate gatling in a CI Pipeline, we have many plugins for different CI tools, and simply use a docker image containing gatling CLI to interact with our cloud.   
Here, we will focus on how to automate checks on key metrics that we want to keep an eye on.  
For that we are going to use **assertions**, you can find all documentation [here](https://gatling.io/docs/gatling/reference/current/core/assertions/) on how to craft them, but here is our example :

```java 
        setUp(scn.injectOpen(constantUsersPerSec(40).during(50))).protocols(HTTP_PROTOCOL)
                .assertions(
                        global().responseTime().percentile4().lt(1000)
                );
```
- global : statistics calculated on all requests  
- percentile4 : By default the 99th percentile
- lt : lower than (in milliseconds)

> We can see in the interface that the assertion has probably failed, and that is something we can use to fail a CI for instance. 

## Conclusion

We had a tour of many ways to use the gatling DSL to simulate as closely as possible users actual behaviour, but of course we could not cover in this workshop the nitty-gritty details of all the concepts. 
Our documentation will cover these thoroughly for the [Gatling DSL](https://gatling.io/docs/gatling/), as well as [Cloud](https://gatling.io/docs/enterprise/cloud/) and [Self-Hosted](https://gatling.io/docs/enterprise/self-hosted/) version.

Thank you for sticking with us until now, hope you had a good time! 

Please, give us some feedback to help us improve this workshop!
https://openfeedback.io/sunnytech2022/2023-06-29/venez-faire-tomber-notre-appli

Xoxo