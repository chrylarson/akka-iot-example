name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.1"

lazy val AkkaVersion = "2.6.9"
lazy val AkkaHttpVersion = "10.2.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "2.2.0",
  "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)
