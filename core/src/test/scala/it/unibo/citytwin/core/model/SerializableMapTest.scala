package it.unibo.citytwin.core.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SerializableMapTest extends AnyWordSpec with Matchers:
  "Serializable map" should {
    "convert correctly a Map" in {
      val map = Map(1 -> "1", 2 -> "2", 3 -> "3")
      val serializableMap = SerializableMap(map)
      serializableMap.toMap shouldBe map
    }
  }
