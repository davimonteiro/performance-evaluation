import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class PerformanceSimulation2 extends Simulation {

    val httpProtocol = http

    val product =
        """
          {
            "id" : null,
            "cartEventType" : "ADD_ITEM",
            "userId" : 1,
            "productId" : "SKU-12464",
            "quantity" : 1
          }
        """

    val users = csv("user_credentials.csv").queue

    val hostIp = "http://localhost"

    val scn = scenario("Performance simulation")
        .repeat(30) {
            //pause(20 seconds)
                feed(users)
                .exec(
                    http("Perform login")
                        .post(hostIp + ":8181/uaa/oauth/token")
                        .header("Authorization", "Basic YWNtZTphY21lc2VjcmV0")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .formParam("username", "${username}")
                        .formParam("password", "${password}")
                        .formParam("grant_type", "password")
                        .formParam("scope", "openid")
                        .formParam("client_id", "acme")
                        .formParam("client_secret", "acmesecret")
                        .check(jsonPath("$.access_token").exists.saveAs("accessToken"))
                        .check(status is 200)
                )
                .pause(10 seconds)
                .exec(
                    http("Add items to the cart")
                        .post(hostIp + ":8957/v1/events")
                        .header("Authorization", "Bearer ${accessToken}")
                        .header("Content-Type", "application/json")
                        .body(StringBody(product)).asJSON
                        .check(status is 200)
                )
                .pause(10 seconds)
                .exec(
                    http("Perform the checkout process")                        
                        .post(hostIp + ":8957/v1/checkout/orchestrated")
                        .header("Authorization", "Bearer ${accessToken}")
                        .header("Content-Type", "application/json")
                        .check(status is 200)
                )
                .pause(5 seconds)
        }

    setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

}