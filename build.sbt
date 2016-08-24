scalaVersion := "2.11.7"

name := "extensionDocker"

crossPaths := false

libraryDependencies +=
  "org.scalatest"  %% "scalatest"  % "2.2.6"  % "test"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.github.spullara.mustache.java" % "compiler"              % "0.9.3",
  "com.github.spullara.mustache.java" % "scala-extensions-2.11" % "0.9.3"
)
