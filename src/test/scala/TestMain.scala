import com.google.inject.Guice
import modules.BaseModule
import org.yaml.snakeyaml.Yaml

object TestMain extends App {
  val stream = getClass.getResourceAsStream("config.yaml")

  val yaml = new Yaml()

  val obj = yaml.load(stream).asInstanceOf[java.util.HashMap[String, Any]]

  val appName = obj.get("appname").asInstanceOf[String]

  println(appName)

  val injector = Guice.createInjector(new BaseModule)


  val KlassAPi = Class.forName("controllers.Api")


  val api = injector.getInstance(KlassAPi.asInstanceOf[Class[_]])

  val klass = api.getClass

  val methods = klass.getMethods

  methods.find(x => x.getName == "test2") match {
    case None => println("none")
    case Some(method) =>
      method.getParameters.foreach {x =>
        println(x.getName)
        println(x.getType.getName)
      }
  }


}
