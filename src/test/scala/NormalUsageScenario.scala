import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class NormalUsageScenario extends Simulation {

  val httpProtocol = http

  val product =
    """
          {
            "cartEventType" : "ADD_ITEM",
            "productNumber" : "SKU-12464",
            "quantity" : 50,
            "accountId" : 1
          }
        """

  val users = csv("user_credentials.csv").queue

  val hostIp = "http://localhost"

  val scn = scenario("Normal usage scenario")
    .repeat(1) {
      feed(users)
        .exec(
          http("Perform login")
            .post(hostIp + ":9999/api/uaa/oauth/token")
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
        .pause(2 seconds)
        .exec(
          http("Add items to the cart")
            .post(hostIp + ":9999/api/shoppingcart/events")
            .header("Authorization", "Bearer ${accessToken}")
            .header("Content-Type", "application/json")
            .body(StringBody(product)).asJSON
            .check(status is 200)
        )
        .pause(2 seconds)
        .exec(
          http("Get the shopping cart")
            .get(hostIp + ":9999/api/shoppingcart/cart")
            .header("Authorization", "Bearer ${accessToken}")
            .check(bodyString.exists.saveAs("cart"))
            .check(status is 200)
        )
        .pause(2 seconds)
        .exec(
          http("Perform the checkout process")
            .get(hostIp + ":9999/api/shoppingcart/checkout")
            .header("Authorization", "Bearer ${accessToken}")
            .body(StringBody("${cart}")).asJSON
            .check(status is 200)
        )
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

}