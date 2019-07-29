package server.mvc.request

import server.libs.typedmap.TypedKey
import server.mvc.{Cookies, Flash, Session}

/**
  * Keys to request attributes.
  */
object RequestAttrKey {

  /**
    * The key for the request attribute storing a request id.
    */
  val Id = TypedKey[Long]("Id")

  /**
    * The key for the request attribute storing a [[Cell]] with
    * [[server.mvc.Cookies]] in it.
    */
  val Cookies = TypedKey[Cell[Cookies]]("Cookies")

  /**
    * The key for the request attribute storing a [[Cell]] with
    * the [[server.mvc.Session]] cookie in it.
    */
  val Session = TypedKey[Cell[Session]]("Session")

  /**
    * The key for the request attribute storing a [[Cell]] with
    * the [[server.mvc.Flash]] cookie in it.
    */
  val Flash = TypedKey[Cell[Flash]]("Flash")
//
//  /**
//    * The key for the request attribute storing the server name.
//    */
//  val Server = TypedKey[String]("Server-Name")
//
//  /**
//    * The CSP nonce key.
//    */
//  val CSPNonce: TypedKey[String] = TypedKey("CSP-Nonce")

}
