package org.nlogo.extensions

import java.io.StringReader

import com.typesafe.config.{ Config, ConfigException, ConfigFactory, ConfigObject, ConfigParseOptions, ConfigSyntax }

import scala.collection.JavaConversions._

case class WarnableValue[A](obtainedValue: A, warnings: Seq[Warning]) {
  def map[B](f: A => B): WarnableValue[B] = WarnableValue(f(obtainedValue), warnings)
  def flatMap[B](f: A => WarnableValue[B]): WarnableValue[B] = {
    val newValue = f(obtainedValue)
    newValue.copy(warnings = warnings ++ newValue.warnings)
  }
}

object HoconParser {
  def apply(s: String): ParsingResult = {
    val parsingConfiguration =
      ConfigParseOptions.defaults.setSyntax(ConfigSyntax.CONF)
    val config = ConfigFactory.parseString(s, parsingConfiguration)
    config.getObjectList("primitives").map(parsePrimitive).foldLeft(ParsingResult(Seq(), Seq())) {
      case (res, WarnableValue(Some(prim), warnings)) => res.copy(primitives = res.primitives :+ prim, warnings = res.warnings ++ warnings)
      case (res, WarnableValue(_, warnings)) => res.copy(warnings = res.warnings ++ warnings)
    }
  }

  def warnableValue[A](c: ConfigObject, key: String, message: String, f: (Config, String) => A, default: A): WarnableValue[A] = {
      try {
        WarnableValue(f(c.toConfig, key), Seq())
      } catch {
        case missing: ConfigException.Missing =>
          val lineNumber = c.origin.lineNumber
          WarnableValue(default, Seq(Warning(s"Missing $key for primitive on line $lineNumber, $message", lineNumber)))
      }
  }

  def stringToType(s: String): TypeDescription =
    s match {
      case ""           => WildcardType
      case "anything"   => WildcardType
      case "list"       => NetLogoList
      case "string"     => NetLogoString
      case "boolean"    => NetLogoBoolean
      case "number"     => NetLogoNumber
      case "patchset"   => Agentset(Patch)
      case "turtleset"  => Agentset(Turtle)
      case "linkset"    => Agentset(Link)
      case "turtle"     => Agent(Turtle)
      case "patch"      => Agent(Patch)
      case "symbol"     => Symbol
      case "code block" => CodeBlock
      case "command block" => CommandBlock
      case "reference" => ReferenceType
      case "optional command block" => OptionalType
      case s if (s.startsWith("repeatable ")) => Repeatable(stringToType(s.stripPrefix("repeatable ")))
      case other => CustomType(other)
    }

  def parsePrimitive(c: ConfigObject): WarnableValue[Option[Primitive]] = {
    def getString(c: Config, s: String): String = c.getString(s)

    val nameOrError =
      warnableValue(c, "name", "excluding from results",
        (c: Config, s: String) => Option(c.getString(s)), None)

    val descriptionOrError =
      warnableValue(c, "description", "adding empty description",
        (c: Config, s: String) => TextString(c.getString(s)), TextString(""))

    val primitiveType =
      warnableValue(c, "type", "defaulting to command", getString _, "command")
        .flatMap[PrimitiveType](
          procedureKind =>
            if (procedureKind == "command") WarnableValue(Command, Seq())
            else
              warnableValue(c, "returns", "assuming wildcard type", getString _, "")
                .map(stringToType).map(Reporter(_)))

    nameOrError.flatMap((name) =>
        descriptionOrError.flatMap((desc) =>
            primitiveType.map((primType) =>
            name.map(n => Primitive(n, primType, desc, Seq(Seq()))))))
  }
}

