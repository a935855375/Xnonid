package server.mvc

import java.util.Locale

import server.core.utils.CaseInsensitiveOrdered

import scala.collection.immutable.{TreeMap, TreeSet}

class Headers(protected var _headers: Seq[(String, String)]) {
  /**
    * The headers as a sequence of name-value pairs.
    */
  def headers: Seq[(String, String)] = _headers

  /**
    * Checks if the given header is present.
    *
    * @param headerName The name of the header (case-insensitive)
    * @return <code>true</code> if the request did contain the header.
    */
  def hasHeader(headerName: String): Boolean = get(headerName).isDefined

  /**
    * True if this request has a body, so we know if we should trigger body parsing. The base implementation simply
    * checks for the Content-Length or Transfer-Encoding headers, but subclasses (such as fake requests) may return
    * true in other cases so the headers need not be updated to reflect the body.
    */
  def hasBody: Boolean = {
    import server.http.HeaderNames._
    get(CONTENT_LENGTH).exists(_.toLong > 0) || hasHeader(TRANSFER_ENCODING)
  }

  /**
    * Append the given headers
    */
  def add(headers: (String, String)*): Headers = new Headers(this.headers ++ headers)

  /**
    * Retrieves the first header value which is associated with the given key.
    */
  def apply(key: String): String = get(key).getOrElse(scala.sys.error("Header doesn't exist"))

  /**
    * Optionally returns the first header value associated with a key.
    */
  def get(key: String): Option[String] = getAll(key).headOption

  /**
    * Retrieve all header values associated with the given key.
    */
  def getAll(key: String): Seq[String] = toMap.getOrElse(key, Nil)

  /**
    * Remove any headers with the given keys
    */
  def remove(keys: String*): Headers = {
    val keySet = TreeSet(keys: _*)(CaseInsensitiveOrdered)
    new Headers(headers.filterNot { case (name, _) => keySet(name) })
  }

  /**
    * Append the given headers, replacing any existing headers having the same keys
    */
  def replace(headers: (String, String)*): Headers = remove(headers.map(_._1): _*).add(headers: _*)

  /**
    * Retrieve all header keys
    */
  def keys: Set[String] = toMap.keySet

  /**
    * Transform the Headers to a Map
    */
  lazy val toMap: Map[String, Seq[String]] = {
    val builder = TreeMap.newBuilder[String, Seq[String]](CaseInsensitiveOrdered)

    headers.groupBy(_._1.toLowerCase(Locale.ENGLISH)).foreach {
      case (_, headers) =>
        // choose the case of first header as canonical
        builder += headers.head._1 -> headers.map(_._2)
    }

    builder.result()
  }

  /**
    * Transform the Headers to a Map by ignoring multiple values.
    */
  lazy val toSimpleMap: Map[String, String] =
    TreeMap.newBuilder[String, String](CaseInsensitiveOrdered).++=(toMap.mapValues(_.headOption.getOrElse(""))).result()

  /**
    * A headers map with all keys normalized to lowercase
    */
  private lazy val lowercaseMap: Map[String, Set[String]] = toMap
    .map {
      case (name, value) => name.toLowerCase(Locale.ENGLISH) -> value
    }
    .mapValues(_.toSet)

  override def equals(that: Any): Boolean = that match {
    case other: Headers => lowercaseMap == other.lowercaseMap
    case _ => false
  }

  override def hashCode: Int = lowercaseMap.hashCode()

  override def toString: String = headers.toString()
}
