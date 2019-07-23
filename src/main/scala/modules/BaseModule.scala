package modules

import java.util.concurrent.Executors

import com.google.inject.{AbstractModule, Provides, Singleton}

import scala.concurrent.ExecutionContext

class BaseModule extends AbstractModule {
  override def configure(): Unit = {

  }

  @Provides
  @Singleton
  def getExecuteContext: ExecutionContext = {
    // If use fixed pool or fork join pool need to make a make a decision.
    // And the default thread number also should have a default value and can be configurable in config file.
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
  }


}
