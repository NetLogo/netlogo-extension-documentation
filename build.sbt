sbtPlugin := true

scalaVersion := "2.12.12"

version    := "0.8.2"

organization := "org.nlogo"

name       := "netlogo-extension-documentation"

crossPaths := false

isSnapshot := false

licenses   += ("Public Domain", url("http://creativecommons.org/licenses/publicdomain/"))

libraryDependencies +=
  "org.scalatest"  %% "scalatest"  % "3.0.4"  % "test"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.github.spullara.mustache.java" % "compiler"              % "0.9.5",
  "com.github.spullara.mustache.java" % "scala-extensions-2.10" % "0.9.5"
)

bintrayRepository   := "NetLogo-JVM"

bintrayOrganization := Some("netlogo")
