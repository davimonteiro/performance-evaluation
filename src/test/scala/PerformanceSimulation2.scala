import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class PerformanceSimulation2 extends Simulation {

    val httpProtocol = http

    val json =
        """
          {
            "id" : null,
            "cartEventType" : "ADD_ITEM",
            "userId" : 1,
            "productId" : "SKU-12464",
            "quantity" : 2
          }
        """

    val scn = scenario("test")
        .exec(
            http("Perform login")
                .post("http://localhost:8181/uaa/oauth/token")
                .header("Authorization", "Basic YWNtZTphY21lc2VjcmV0")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("username", "user")
                .formParam("password", "password")
                .formParam("grant_type", "password")
                .formParam("scope", "openid")
                .formParam("client_id", "acme")
                .formParam("client_secret", "acmesecret")
                .check(jsonPath("$.access_token").exists.saveAs("accessToken"))
                .check(status is 200)
        )
        .pause(2 seconds)
        .exec(
            http("Add items to the cart")
                .post("http://localhost:8957/v1/events")
                .header("Authorization", "Bearer ${accessToken}")
                .header("Content-Type", "application/json")
                .body(StringBody(json)).asJSON
                .check(status is 200)
        )
        .pause(2 seconds)
        .exec(
            http("Perform the checkout process")
                .post("http://localhost:8957/v1/checkout/orchestrated")
                .header("Authorization", "Bearer ${accessToken}")
                .check(status is 200)
        )
        /*.pause(2 seconds)
        .exec(
            http("Perform the orchestrated checkout process")
                .post("http://localhost:8957/v1/checkout")
                .header("Authorization", "Bearer ${accessToken}")
                .check(status is 200)
        )*/

    setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

}