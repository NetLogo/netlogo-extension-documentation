sbtPlugin := true

scalaVersion := "2.12.12"

version := "0.8.3"

organization := "org.nlogo"

name := "netlogo-extension-documentation"

crossPaths := false

isSnapshot := false

licenses += ("Creative Commons Zero v1.0 Universal Public Domain Dedication", url("https://creativecommons.org/publicdomain/zero/1.0/"))

libraryDependencies +=
  "org.scalatest"  %% "scalatest"  % "3.0.4"  % "test"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.github.spullara.mustache.java" % "compiler"              % "0.9.5",
  "com.github.spullara.mustache.java" % "scala-extensions-2.10" % "0.9.5"
)

publishTo := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/netlogo-extension-documentation/") }
