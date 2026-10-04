ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "RestaurantAnalytics",
    version := "0.1.0",

    libraryDependencies ++= Seq(
      "org.mongodb.scala" % "mongo-scala-driver_2.13" % "5.6.1"
    )
  )