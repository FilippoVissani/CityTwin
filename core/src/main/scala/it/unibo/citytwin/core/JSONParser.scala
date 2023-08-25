package it.unibo.citytwin.core

import it.unibo.citytwin.core.model.{MainstayState, Resource}
import play.api.libs.json.{JsValue, Json}
import java.time.LocalDateTime

/** JSONParser is the object that parses the JSON responses from the persistence service
  */
object JSONParser:

  /** jsonToMainstaysHistory is the function that parses the JSON response from the persistence
    * service to a sequence of MainstayState
    * @param jsonString
    *   the JSON response from the persistence service
    * @return
    *   the sequence of MainstayState
    */
  def jsonToMainstaysHistory(jsonString: String): Seq[(MainstayState, LocalDateTime)] =
    val json: JsValue    = Json.parse(jsonString)
    val addressesHistory = (json \\ "address").map(v => v.asOpt[String])
    val statesHistory    = (json \\ "state").map(v => v.asOpt[Boolean])
    val timeHistory      = parseTimeHistory(json)
    val mergedHistory: Seq[(MainstayState, LocalDateTime)] = addressesHistory
      .zip(statesHistory)
      .map((a, s) => MainstayState(address = a, state = s))
      .zip(timeHistory)
      .filter((m, t) => m.address.isDefined && m.state.isDefined && t.isDefined)
      .map((m, t) => (m, t.get))
      .toSeq
    mergedHistory

  /** jsonToResourcesHistory is the function that parses the JSON response from the persistence
    * service to a sequence of Resource
    * @param jsonString
    *   the JSON response from the persistence service
    * @return
    *   the sequence of Resource
    */
  def jsonToResourcesHistory(jsonString: String): Seq[(Resource, LocalDateTime)] =
    val json: JsValue = Json.parse(jsonString)
    val namesHistory  = (json \\ "name").map(v => v.asOpt[String])
    val statesHistory = (json \\ "node_state").map(v => v.asOpt[Boolean])
    val timeHistory   = parseTimeHistory(json)
    val mergedHistory: Seq[(Resource, LocalDateTime)] = namesHistory
      .zip(statesHistory)
      .map((n, s) => Resource(name = n, nodeState = s))
      .zip(timeHistory)
      .filter((r, t) => r.name.isDefined && r.nodeState.isDefined && t.isDefined)
      .map((r, t) => (r, t.get))
      .toSeq
    mergedHistory

  private def parseTimeHistory(
      json: JsValue
  ): scala.collection.Seq[Option[java.time.LocalDateTime]] =
    (json \\ "time").map(v => v.asOpt[String].map(x => LocalDateTime.parse(x)))
