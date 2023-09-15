package it.unibo.citytwin.core

object PersistenceServiceAPISpec:

  enum Scheme(val scheme: String):
    case HTTP extends Scheme("http")
    case HTTPS extends Scheme("https")

  enum Resource(val resource: String):
    case Mainstays extends Resource("/mainstays")
    case Resources extends Resource("/resources")

  def generateURI(scheme: Scheme, host: String, port: String, resource: Resource): String =
    s"${scheme.scheme}://$host:$port${resource.resource}"
    