package it.unibo.citytwin.core.model

import java.time.LocalDateTime

/** MainstayState is the class that represents the state of a Mainstay Actor
  *
  * @param address
  *   the address of the Mainstay Actor
  * @param state
  *   the state of the Mainstay Actor
  */
case class MainstayState(address: String, state: Boolean, time: LocalDateTime)
