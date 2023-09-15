package it.unibo.citytwin.core

import it.unibo.citytwin.core.model.MainstayState
import it.unibo.citytwin.core.model.ResourceState
import play.api.libs.json.JsValue
import play.api.libs.json.Json

import java.time.LocalDateTime

/** JSONParser is the object that parses the JSON responses from the persistence service
  */
object JSONDeSerializer:

  /** jsonToMainstaysHistory is the function that parses the JSON response from the persistence
    * service to a sequence of MainstayState
    * @param jsonString
    *   the JSON response from the persistence service
    * @return
    *   the sequence of MainstayState
    */
  def jsonToMainstaysHistory(jsonString: String): Seq[MainstayState] =
    val json: JsValue    = Json.parse(jsonString)
    val addressesHistory = (json \\ "address").map(v => v.asOpt[String])
    val statesHistory    = (json \\ "state").map(v => v.asOpt[Boolean])
    val timeHistory      = parseTimeHistory(json)
    val mergedHistory: Seq[MainstayState] =
      List(addressesHistory, statesHistory, timeHistory).transpose
        .filter(x => x.forall(y => y.isDefined))
        .map(x =>
          MainstayState(
            x.head.get.asInstanceOf[String],
            x(1).get.asInstanceOf[Boolean],
            x(2).get.asInstanceOf[LocalDateTime]
          )
        )
    mergedHistory

  /** jsonToResourcesHistory is the function that parses the JSON response from the persistence
    * service to a sequence of Resource
    * @param jsonString
    *   the JSON response from the persistence service
    * @return
    *   the sequence of Resource
    */
  def jsonToResourcesHistory(jsonString: String): Seq[ResourceState] =
    val json: JsValue = Json.parse(jsonString)
    val namesHistory  = (json \\ "name").map(v => v.asOpt[String])
    val statesHistory = (json \\ "node_state").map(v => v.asOpt[Boolean])
    val timeHistory   = parseTimeHistory(json)
    val mergedHistory: Seq[ResourceState] =
      List(namesHistory, statesHistory, timeHistory).transpose
        .filter(x => x.forall(y => y.isDefined))
        .map(x =>
          ResourceState(
            name = x.head.asInstanceOf[Option[String]],
            nodeState = x(1).asInstanceOf[Option[Boolean]],
            time = x(2).asInstanceOf[Option[LocalDateTime]]
          )
        )
    mergedHistory
    
  def resourceToJson(address: String, resource: ResourceState): String =
    var body = Json.obj(
      "address" -> address,
      "name" -> resource.name.orNull,
      "state" -> resource.state.orNull,
      "resource_type" -> resource.resourceType,
      "time" -> resource.time.orNull
    )
    if resource.position.isDefined then
      body = body ++ Json.obj(
        "position_x" -> resource.position.get.x,
        "position_y" -> resource.position.get.y
      )
    if resource.nodeState.isDefined then
      body = body ++ Json.obj("node_state" -> resource.nodeState.get)
    body.toString()
    
  def mainstayToJson(mainstay: MainstayState): String =
    Json.obj(
        "address" -> mainstay.address,
        "state" -> mainstay.state,
        "time" -> mainstay.time
      ).toString()

  private def parseTimeHistory(
      json: JsValue
  ): scala.collection.Seq[Option[java.time.LocalDateTime]] =
    (json \\ "time").map(v => v.asOpt[String].map(x => LocalDateTime.parse(x)))
