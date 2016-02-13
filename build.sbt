name := """psmith"""

version := "1.0"

scalaVersion := "2.11.6"

resolvers += "spray repo" at "http://repo.spray.io"

val sprayVersion = "1.3.2"

val akkaVersion = "2.4.2-RC1"

val akkaStreamVersion = "2.0.3"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

Revolver.settings

//resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "com.github.scopt"       %% "scopt"                 % "3.3.0",
  "com.typesafe.akka"      %% "akka-actor"            % akkaVersion,
  "com.typesafe.akka"      %% "akka-stream-experimental" % akkaStreamVersion,
  "com.typesafe.akka"      %% "akka-http-core-experimental" % akkaStreamVersion,
  "com.typesafe.akka"      %% "akka-http-experimental" % akkaStreamVersion,
  //"io.spray"               %% "spray-caching"         % sprayVersion,
  "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.2",
  "com.github.nscala-time" %% "nscala-time"           % "1.8.0",
  "com.github.ancane"      %% "haldr"                 % "0.1",
  "org.json4s"             %% "json4s-jackson"        % "3.2.10",
  "org.json4s"             %% "json4s-ext"            % "3.2.10",
  "io.spray"               %% "spray-testkit"         % sprayVersion   % "test",
  "nl.grons"               %% "metrics-scala"         % "3.5.2_a2.3"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

//parallelExecution in Test := false

//fork in run := true

//connectInput in run := true

//outputStrategy in run := Some(StdoutOutput)
