package it.unibo.citytwin.core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

class JsonParseTest extends AnyWordSpec with Matchers:

  "Parser" should {
    "Parse correctly a JSON string" in {
      val jsonString = "[\n    {\n        \"_id\": \"64e780ad30d4ded1359bf52e\",\n        \"address\": \"akka://ClusterSystem@127.0.0.1:2551/user\",\n        \"state\": true,\n        \"time\": \"2023-08-24T18:09:17.581426074\"\n    },\n    {\n        \"_id\": \"64e780ae30d4ded1359bf534\",\n        \"address\": \"akka://ClusterSystem@127.0.0.1:2551/user\",\n        \"state\": true,\n        \"time\": \"2023-08-24T18:09:17.996343216\"\n    },\n    {\n        \"_id\": \"64e780b030d4ded1359bf546\",\n        \"address\": \"akka://ClusterSystem@127.0.0.1:2551/user\",\n        \"state\": true,\n        \"time\": \"2023-08-24T18:09:20.073069294\"\n    },\n    {\n        \"_id\": \"64e780b130d4ded1359bf555\",\n        \"address\": \"akka://ClusterSystem@127.0.0.1:2551/user\",\n        \"state\": true,\n        \"time\": \"2023-08-24T18:09:21.739939386\"\n    }\n]"
      JSONParser.jsonToMainstaysHistory(jsonString).map((x, y) => (x, y.toString)) shouldBe List((true, "2023-08-24T18:09:17.581426074"), (true, "2023-08-24T18:09:17.996343216"), (true, "2023-08-24T18:09:20.073069294"), (true, "2023-08-24T18:09:21.739939386"))
    }
  }
