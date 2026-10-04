object Main {

  def main(args: Array[String]): Unit = {

    println("==============================================")
    println("   RESTAURANT DISCOVERY & ANALYTICS SYSTEM")
    println("==============================================")

    println()
    println("Starting Scala web application...")

    WebServer.start(8080)
  }
}