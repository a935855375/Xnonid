class Puppy(Txt: String) {
  println(Txt)
}

object Puppy {
  def main(args: Array[String]): Unit = {
    val myPuppy = new Puppy("如初")
  }
}
