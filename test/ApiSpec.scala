import controllers.{ApiCallException, ApiResult}
import org.specs2.mutable._

import play.api.libs.json._
import play.api.libs.json.JsArray
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Future

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApiSpec extends Specification {
  "ApiResult" should {
    "wrap data with status on a successful response" in {
      implicit val request = FakeRequest(GET, "/test")
      val success = ApiResult.noSource {
        Json.obj("test" -> "value")
      }
      contentType(success) must beSome("application/json")
      status(success) must equalTo(OK)
      contentAsJson(success) \ "status" mustEqual JsString("success")
    }

    "wrap data with fail when an Api exception is thrown" in {
      implicit val request = FakeRequest(GET, "/test")
      val fail = ApiResult.noSource {
        if (true) throw ApiCallException(Json.obj("test" -> "just testing the fail state"))
        Json.obj("never" -> "reached")
      }
      contentType(fail) must beSome("application/json")
      status(fail) must equalTo(BAD_REQUEST)
      contentAsJson(fail) \ "status" mustEqual JsString("fail")
      contentAsJson(fail) \ "data" mustEqual Json.obj("test" -> "just testing the fail state")
    }

    "return an error when something else goes wrong" in {
      implicit val request = FakeRequest(GET, "/test")
      val error = ApiResult.noSource {
        Json.obj("infinity" -> (1 / 0))
      }
      contentType(error) must beSome("application/json")
      status(error) must equalTo(INTERNAL_SERVER_ERROR)
      contentAsJson(error) \ "status" mustEqual JsString("error")
      contentAsJson(error) \ "message" mustEqual JsString("/ by zero")
    }

    "add a length companion field to arrays contained in objects when requested" in {
      implicit val request = FakeRequest(GET, "/test?_length=true")
      val success = ApiResult.noSource {
        Json.obj("test" -> List("first", "second", "third"))
      }
      contentAsJson(success) \ "data" \ "test.length" mustEqual JsNumber(3)
    }
  }

  "Application" should {
    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boom")) must beNone
    }

    "return a list of instances" in new WithApplication{
      val home = route(FakeRequest(GET, "/instances")).get
      status(home) must equalTo(OK)
      contentType(home) must beSome("application/json")
      val jsonInstances = contentAsJson(home) \ "data" \ "instances"
      jsonInstances must beLike { case JsArray(_) => ok }
      jsonInstances.as[JsArray].value.length mustEqual 15
    }

    "filter a list of instances" in new WithApplication {
      val home = route(FakeRequest(GET, "/instances?vendor=aws")).get
      val jsonInstances = contentAsJson(home) \ "data" \ "instances"
      jsonInstances.as[JsArray].value.length mustEqual 8
    }

    "invert filter a list of instances" in new WithApplication {
      val home = route(FakeRequest(GET, "/instances?vendor!=aws")).get
      val jsonInstances = contentAsJson(home) \ "data" \ "instances"
      jsonInstances.as[JsArray].value.length mustEqual 7
    }

    "filter a list of instances using a regex" in new WithApplication {
      val home = route(FakeRequest(GET, "/instances?mainclasses~=.*db.*")).get
      val jsonInstances = contentAsJson(home) \ "data" \ "instances"
      jsonInstances.as[JsArray].value.length mustEqual 6
    }

    "filter a list of instances by nested field" in new WithApplication {
      val home = route(FakeRequest(GET, "/instances?tags.App=db")).get
      val jsonInstances = contentAsJson(home) \ "data" \ "instances"
      jsonInstances.as[JsArray].value.length mustEqual 3
    }
  }
}
