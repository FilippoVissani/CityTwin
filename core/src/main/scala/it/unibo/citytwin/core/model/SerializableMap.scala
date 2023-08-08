package it.unibo.citytwin.core.model

trait SerializableMap[K, V]:
  def toMap: Map[K, V]

object SerializableMap:
  def apply[K, V](map: Map[K, V]): SerializableMap[K, V] = SerializableMapImpl(map)

  private class SerializableMapImpl[K, V](map: Map[K, V]) extends SerializableMap[K, V]:
    private val pairs: Set[(K, V)] = map.toSet

    override def toMap: Map[K, V] = Map.from(pairs)
