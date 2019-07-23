package services

import com.google.inject.Singleton
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FooService @Inject()(implicit ec: ExecutionContext) {


  def sayHello: Future[String] = Future {
    "hello world"
  }
}
