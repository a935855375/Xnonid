package server

import java.io.{File, InputStream}

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}
import com.google.inject.{Inject, Singleton}
import server.ApplicationLoader.DevContext
import server.core.{DefaultWebCommands, SourceMapper, WebCommands}
import server.http.HttpRequestHandler.DefaultHttpRequestHandler
import server.http._
import server.i18n.I18nComponents
import server.inject._
import server.libs.Files._
import server.libs.concurrent.{ActorSystemProvider, CoordinatedShutdownProvider, CoordinatedShutdownSupport}
import server.libs.crypto.{CSRFTokenSigner, CSRFTokenSignerProvider, CookieSigner, CookieSignerProvider}
import server.mvc.request.{DefaultRequestFactory, RequestFactory}
import server.mvc._
import server.routing.Router
import server.utils.InlineCache

import scala.annotation.implicitNotFound
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * A Play application.
  *
  * Application creation is handled by the framework engine.
  *
  * If you need to create an ad-hoc application,
  * for example in case of unit testing, you can easily achieve this using:
  * {{{
  * val application = new DefaultApplication(new File("."), this.getClass.getClassloader, None, Play.Mode.Dev)
  * }}}
  *
  * This will create an application using the current classloader.
  *
  */
@implicitNotFound(
  msg = "You do not have an implicit Application in scope. If you want to bring the current running Application into context, please use dependency injection."
)
trait Application {

  /**
    * The absolute path hosting this application, mainly used by the `getFile(path)` helper method
    */
  def path: File

  /**
    * The application's classloader
    */
  def classloader: ClassLoader

  /**
    * `Dev`, `Prod` or `Test`
    */
  def mode: Mode = environment.mode

  /**
    * The application's environment
    */
  def environment: Environment

  private[server] def isDev = mode == Mode.Dev

  private[server] def isTest = mode == Mode.Test

  private[server] def isProd = mode == Mode.Prod

  def configuration: Configuration

  private[server] lazy val httpConfiguration = HttpConfiguration.fromConfiguration(configuration, environment)

  /**
    * The default ActorSystem used by the application.
    */
  def actorSystem: ActorSystem

  /**
    * The default Materializer used by the application.
    */
  implicit def materializer: Materializer

  /**
    * The default CoordinatedShutdown to stop the Application
    */
  def coordinatedShutdown: CoordinatedShutdown

  /**
    * The factory used to create requests for this application.
    */
  def requestFactory: RequestFactory


  /**
    * The HTTP request handler
    */
  def requestHandler: HttpRequestHandler

  /**
    * The HTTP error handler
    */
  def errorHandler: HttpErrorHandler

  /**
    * Retrieves a file relative to the application root path.
    *
    * Note that it is up to you to manage the files in the application root path in production.  By default, there will
    * be nothing available in the application root path.
    *
    * For example, to retrieve some deployment specific data file:
    * {{{
    * val myDataFile = application.getFile("data/data.xml")
    * }}}
    *
    * @param relativePath relative path of the file to fetch
    * @return a file instance; it is not guaranteed that the file exists
    */
  @deprecated("Use Environment#getFile instead", "2.6.0")
  def getFile(relativePath: String): File = new File(path, relativePath)

  /**
    * Retrieves a file relative to the application root path.
    * This method returns an Option[File], using None if the file was not found.
    *
    * Note that it is up to you to manage the files in the application root path in production.  By default, there will
    * be nothing available in the application root path.
    *
    * For example, to retrieve some deployment specific data file:
    * {{{
    * val myDataFile = application.getExistingFile("data/data.xml")
    * }}}
    *
    * @param relativePath the relative path of the file to fetch
    * @return an existing file
    */
  @deprecated("Use Environment#getExistingFile instead", "2.6.0")
  def getExistingFile(relativePath: String): Option[File] = Some(getFile(relativePath)).filter(_.exists)

  /**
    * Scans the application classloader to retrieve a resource.
    *
    * The conf directory is included on the classpath, so this may be used to look up resources, relative to the conf
    * directory.
    *
    * For example, to retrieve the conf/logback-dev.xml configuration file:
    * {{{
    * val maybeConf = application.resource("logback-dev.xml")
    * }}}
    *
    * @param name the absolute name of the resource (from the classpath root)
    * @return the resource URL, if found
    */
  @deprecated("Use Environment#resource instead", "2.6.0")
  def resource(name: String): Option[java.net.URL] = {
    val n = name.stripPrefix("/")
    Option(classloader.getResource(n))
  }

  /**
    * Scans the application classloader to retrieve a resource’s contents as a stream.
    *
    * The conf directory is included on the classpath, so this may be used to look up resources, relative to the conf
    * directory.
    *
    * For example, to retrieve the conf/logback-dev.xml configuration file:
    * {{{
    * val maybeConf = application.resourceAsStream("logback-dev.xml")
    * }}}
    *
    * @param name the absolute name of the resource (from the classpath root)
    * @return a stream, if found
    */
  @deprecated("Use Environment#resourceAsStream instead", "2.6.0")
  def resourceAsStream(name: String): Option[InputStream] = {
    val n = name.stripPrefix("/")
    Option(classloader.getResourceAsStream(n))
  }

  /**
    * Stop the application.  The returned future will be redeemed when all stop hooks have been run.
    */
  def stop(): Future[_]

  /**
    * Get the runtime injector for this application. In a runtime dependency injection based application, this can be
    * used to obtain components as bound by the DI framework.
    *
    * @return The injector.
    */
  def injector: Injector = NewInstanceInjector

  /**
    * Returns true if the global application is enabled for this app. If set to false, this changes the behavior of
    * Play.start, Play.current, and Play.maybeApplication to disallow access to the global application instance,
    * also affecting the deprecated Play APIs that use these.
    */
  lazy val globalApplicationEnabled: Boolean = {
    configuration.getOptional[Boolean](Server.GlobalAppConfigKey).getOrElse(true)
  }
}

object Application {
  /**
    * Creates a function that caches results of calls to
    * `app.injector.instanceOf[T]`. The cache speeds up calls
    * when called with the same Application each time, which is
    * a big benefit in production. It still works properly if
    * called with a different Application each time, such as
    * when running unit tests, but it will run more slowly.
    *
    * Since values are cached, it's important that this is only
    * used for singleton values.
    *
    * This method avoids synchronization so it's possible that
    * the injector might be called more than once for a single
    * instance if this method is called from different threads
    * at the same time.
    *
    * The cache uses a SoftReference to both the Application and
    * the returned instance so it will not cause memory leaks.
    * Unlike WeakHashMap it doesn't use a ReferenceQueue, so values
    * will still be cleaned even if the ReferenceQueue is never
    * activated.
    */
  def instanceCache[T: ClassTag]: Application => T =
    new InlineCache((app: Application) => app.injector.instanceOf[T])
}

@Singleton
class DefaultApplication @Inject()(
                                    override val environment: Environment,
                                    applicationLifecycle: ApplicationLifecycle,
                                    override val injector: Injector,
                                    override val configuration: Configuration,
                                    override val requestFactory: RequestFactory,
                                    override val requestHandler: HttpRequestHandler,
                                    override val errorHandler: HttpErrorHandler,
                                    override val actorSystem: ActorSystem,
                                    override val materializer: Materializer,
                                    override val coordinatedShutdown: CoordinatedShutdown
                                  ) extends Application {

  def this(
            environment: Environment,
            applicationLifecycle: ApplicationLifecycle,
            injector: Injector,
            configuration: Configuration,
            requestFactory: RequestFactory,
            requestHandler: HttpRequestHandler,
            errorHandler: HttpErrorHandler,
            actorSystem: ActorSystem,
            materializer: Materializer
          ) = this(
    environment,
    applicationLifecycle,
    injector,
    configuration,
    requestFactory,
    requestHandler,
    errorHandler,
    actorSystem,
    materializer,
    new CoordinatedShutdownProvider(actorSystem, applicationLifecycle).get
  )

  override def path: File = environment.rootPath

  override def classloader: ClassLoader = environment.classLoader

  override def stop(): Future[_] = CoordinatedShutdownSupport.asyncShutdown(actorSystem, ApplicationStoppedReason)
}

private[server] final case object ApplicationStoppedReason extends CoordinatedShutdown.Reason

/**
  * Helper to provide the Play built in components.
  */
trait BuiltInComponents extends I18nComponents {

  /** The application's environment, e.g. it's [[ClassLoader]] and root path. */
  def environment: Environment

  /** Helper to locate the source code for the application. Only available in dev mode. */
  @deprecated("Use devContext.map(_.sourceMapper) instead", "2.7.0")
  def sourceMapper: Option[SourceMapper] = devContext.map(_.sourceMapper)

  /** Helper to interact with the Play build environment. Only available in dev mode. */
  def devContext: Option[DevContext] = None

  // Define a private val so that webCommands can remain a `def` instead of a `val`
  private val defaultWebCommands: WebCommands = new DefaultWebCommands

  /** Commands that intercept requests before the rest of the application handles them. Used by Evolutions. */
  def webCommands: WebCommands = defaultWebCommands

  /** The application's configuration. */
  def configuration: Configuration

  /** A registry to receive application lifecycle events, e.g. to close resources when the application stops. */
  def applicationLifecycle: ApplicationLifecycle

  /** The router that's used to pass requests to the correct handler. */
  def router: Router

  /**
    * The runtime [[Injector]] instance provided to the [[DefaultApplication]]. This injector is set up to allow
    * existing (deprecated) legacy APIs to function. It is not set up to support injecting arbitrary Play components.
    */
  lazy val injector: Injector = {
    val simple = new SimpleInjector(NewInstanceInjector) +
      cookieSigner + // play.api.libs.Crypto (for cookies)
      httpConfiguration + // play.api.mvc.BodyParsers trait
      tempFileCreator + // play.api.libs.TemporaryFileCreator object
      messagesApi + // play.api.i18n.Messages object
      langs // play.api.i18n.Langs object
    new ContextClassLoaderInjector(simple, environment.classLoader)
  }

  lazy val playBodyParsers: PlayBodyParsers =
    PlayBodyParsers(tempFileCreator, httpErrorHandler, httpConfiguration.parser)(materializer)
  lazy val defaultBodyParser: BodyParser[AnyContent] = playBodyParsers.default
  lazy val defaultActionBuilder: DefaultActionBuilder = DefaultActionBuilder(defaultBodyParser)

  lazy val httpConfiguration: HttpConfiguration = HttpConfiguration.fromConfiguration(configuration, environment)
  lazy val requestFactory: RequestFactory = new DefaultRequestFactory(httpConfiguration)
  lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(environment, configuration, devContext.map(_.sourceMapper), Some(router))

  /**
    * List of filters, typically provided by mixing in play.filters.HttpFiltersComponents
    * or play.api.NoHttpFiltersComponents.
    *
    * In most cases you will want to mixin HttpFiltersComponents and append your own filters:
    *
    * {{{
    * class MyComponents(context: ApplicationLoader.Context)
    *   extends BuiltInComponentsFromContext(context)
    *   with play.filters.HttpFiltersComponents {
    *
    *   lazy val loggingFilter = new LoggingFilter()
    *   override def httpFilters = {
    *     super.httpFilters :+ loggingFilter
    *   }
    * }
    * }}}
    *
    * If you want to filter elements out of the list, you can do the following:
    *
    * {{{
    * class MyComponents(context: ApplicationLoader.Context)
    *   extends BuiltInComponentsFromContext(context)
    *   with play.filters.HttpFiltersComponents {
    *   override def httpFilters = {
    *     super.httpFilters.filterNot(_.getClass == classOf[CSRFFilter])
    *   }
    * }
    * }}}
    */
  def httpFilters: Seq[EssentialFilter]

  lazy val httpRequestHandler: HttpRequestHandler =
    new DefaultHttpRequestHandler(webCommands, devContext, router, httpErrorHandler, httpConfiguration, httpFilters)

  lazy val application: Application = new DefaultApplication(
    environment,
    applicationLifecycle,
    injector,
    configuration,
    requestFactory,
    httpRequestHandler,
    httpErrorHandler,
    actorSystem,
    materializer,
    coordinatedShutdown
  )

  lazy val actorSystem: ActorSystem = new ActorSystemProvider(environment, configuration).get
  implicit lazy val materializer: Materializer = ActorMaterializer()(actorSystem)
  lazy val coordinatedShutdown: CoordinatedShutdown =
    new CoordinatedShutdownProvider(actorSystem, applicationLifecycle).get
  implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  lazy val cookieSigner: CookieSigner = new CookieSignerProvider(httpConfiguration.secret).get

  lazy val csrfTokenSigner: CSRFTokenSigner = new CSRFTokenSignerProvider(cookieSigner).get

  lazy val tempFileReaper: TemporaryFileReaper =
    new DefaultTemporaryFileReaper(actorSystem, TemporaryFileReaperConfiguration.fromConfiguration(configuration))
  lazy val tempFileCreator: TemporaryFileCreator = new DefaultTemporaryFileCreator(applicationLifecycle, tempFileReaper)

  lazy val fileMimeTypes: FileMimeTypes = new DefaultFileMimeTypesProvider(httpConfiguration.fileMimeTypes).get

  // NOTE: the following helpers are declared as protected since they are only meant to be used inside BuiltInComponents
  // This also makes them not conflict with other methods of the same type when used with Macwire.

  /**
    * Alias method to [[defaultActionBuilder]]. This just helps to keep the idiom of using `Action`
    * when creating `Router`s using the built in components.
    *
    * @return the default action builder.
    */
  protected def Action: DefaultActionBuilder = defaultActionBuilder

  /**
    * Alias method to [[playBodyParsers]].
    */
  protected def parse: PlayBodyParsers = playBodyParsers
}

/**
  * A component to mix in when no default filters should be mixed in to BuiltInComponents.
  *
  * @see [[BuiltInComponents.httpFilters]]
  */
trait NoHttpFiltersComponents {
  val httpFilters: Seq[EssentialFilter] = Nil
}
