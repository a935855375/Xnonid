package server.i18n

import server.http.HttpConfiguration
import server.{Configuration, Environment}
import server.inject.Module

class I18nModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[Langs].toProvider[DefaultLangsProvider],
      bind[MessagesApi].toProvider[DefaultMessagesApiProvider],
      bind[server.i18n.MessagesApi].toSelf,
      bind[server.i18n.Langs].toSelf
    )
  }
}

/**
  * Injection helper for i18n components
  */
trait I18nComponents {

  def environment: Environment
  def configuration: Configuration
  def httpConfiguration: HttpConfiguration

  lazy val langs: Langs = new DefaultLangsProvider(configuration).get
  lazy val messagesApi: MessagesApi =
    new DefaultMessagesApiProvider(environment, configuration, langs, httpConfiguration).get

}
