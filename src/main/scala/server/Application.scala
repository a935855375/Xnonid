package server

import java.io.{File, InputStream}

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.Materializer
import server.core.utils.InlineCache
import server.http.HttpConfiguration
import server.inject.{Injector, NewInstanceInjector}

import scala.annotation.implicitNotFound
import scala.concurrent.Future
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

  //  /**
  //    * The factory used to create requests for this application.
  //    */
  //  def requestFactory: RequestFactory
  //
  //  /**
  //    * The HTTP request handler
  //    */
  //  def requestHandler: HttpRequestHandler
  //
  //  /**
  //    * The HTTP error handler
  //    */
  //  def errorHandler: HttpErrorHandler

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
    * For example, to retrieve the conf/logback.xml configuration file:
    * {{{
    * val maybeConf = application.resource("logback.xml")
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
    * For example, to retrieve the conf/logback.xml configuration file:
    * {{{
    * val maybeConf = application.resourceAsStream("logback.xml")
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
