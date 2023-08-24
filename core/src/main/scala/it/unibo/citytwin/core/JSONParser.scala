package it.unibo.citytwin.core

import it.unibo.citytwin.core.model.Resource
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDateTime

object JSONParser:

  def jsonToMainstaysHistory(jsonString: String): Seq[(Boolean, LocalDateTime)] =
    val json: JsValue = Json.parse(jsonString)
    val statesHistory = (json \\ "state").map(v => v.asOpt[Boolean])
    val datesHistory  = (json \\ "time").map(v => v.asOpt[String].map(x => LocalDateTime.parse(x)))
    val mergedHistory: Seq[(Boolean, LocalDateTime)] = statesHistory
      .zip(datesHistory)
      .filter((s, t) => s.isDefined && t.isDefined)
      .map((s, t) => (s.get, t.get))
      .toSeq
    mergedHistory

  def jsonToResourcesHistory(jsonString: String): Seq[(Boolean, LocalDateTime)] =
    val json: JsValue = Json.parse(jsonString)
    val statesHistory = (json \\ "node_state").map(v => v.asOpt[Boolean])
    val datesHistory  = (json \\ "time").map(v => v.asOpt[String].map(x => LocalDateTime.parse(x)))
    val mergedHistory: Seq[(Boolean, LocalDateTime)] = statesHistory
      .zip(datesHistory)
      .filter((s, t) => s.isDefined && t.isDefined)
      .map((s, t) => (s.get, t.get))
      .toSeq
    mergedHistory
