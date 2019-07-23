package server.mvc

import akka.util.ByteString

object Codec {

  /**
    * Create a Codec from an encoding already supported by the JVM.
    */
  def javaSupported(charset: String) =
    Codec(charset)(str => ByteString.apply(str, charset), bytes => bytes.decodeString(charset))

  /**
    * Codec for UTF-8
    */
  implicit val utf_8 = javaSupported("utf-8")

  /**
    * Codec for ISO-8859-1
    */
  val iso_8859_1 = javaSupported("iso-8859-1")

}

case class Codec(charset: String)(val encode: String => ByteString, val decode: ByteString => String)

