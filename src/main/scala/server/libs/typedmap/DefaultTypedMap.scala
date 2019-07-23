/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package server.libs.typedmap

import scala.annotation.varargs
import scala.collection.immutable


/**
  * An implementation of `TypedMap` that wraps a standard Scala [[Map]].
  */
private[typedmap] final class DefaultTypedMap private[typedmap](m: immutable.Map[TypedKey[_], Any]) extends TypedMap {
  override def apply[A](key: TypedKey[A]): A = m.apply(key).asInstanceOf[A]

  override def get[A](key: TypedKey[A]): Option[A] = m.get(key).asInstanceOf[Option[A]]

  override def contains(key: TypedKey[_]): Boolean = m.contains(key)

  override def updated[A](key: TypedKey[A], value: A): TypedMap = new DefaultTypedMap(m.updated(key, value))

  override def +(entries: TypedEntry[_]*): TypedMap = {
    val m2 = entries.foldLeft(m) {
      case (m1, e) => m1.updated(e.key, e.value)
    }
    new DefaultTypedMap(m2)
  }

  @varargs def -(keys: TypedKey[_]*): TypedMap = {
    val m2 = keys.foldLeft(m) {
      case (m1, k) => m1 - k
    }
    new DefaultTypedMap(m2)
  }

  override def toString: String = m.mkString("{", ", ", "}")
}
