package org.nlogo.build

import java.io.{ File, StringReader }

import com.typesafe.config.{ Config, ConfigException, ConfigFactory, ConfigObject, ConfigParseOptions, ConfigSyntax }

import scala.collection.JavaConverters._

case class WarnableValue[+A](obtainedValue: A, warnings: Seq[Warning]) {
  def map[B](f: A => B): WarnableValue[B] = WarnableValue(f(obtainedValue), warnings)
  def flatMap[B](f: A => WarnableValue[B]): WarnableValue[B] = {
    val newValue = f(obtainedValue)
    newValue.copy(warnings = warnings ++ newValue.warnings)
  }

  def merge[B, C](otherWarnable: WarnableValue[B], f: (A, B) => C): WarnableValue[C] = {
    otherWarnable.flatMap(
      otherValue => new WarnableValue(f(obtainedValue, otherValue),
        warnings ++ otherWarnable.warnings))
  }
}

object HoconParser {
  val parsingConfiguration =
    ConfigParseOptions.defaults.setSyntax(ConfigSyntax.CONF)

  def parseConfigFile(f: File): Config =
    ConfigFactory.parseFile(f, parsingConfiguration)

  def parseConfigText(s: String): Config =
    ConfigFactory.parseString(s, parsingConfiguration)

  def parsePrimitives(config: Config): ParsingResult = {
    val extensionName = defaultValue(config, "extensionName", getString _, "")
    config.getConfigList("primitives").asScala
      .map(parsePrimitive(extensionName))
      .foldLeft(ParsingResult(Seq(), Seq())) {
        case (res, WarnableValue(Some(prim), warnings)) =>
          res.copy(primitives = res.primitives :+ prim, warnings = res.warnings ++ warnings)
        case (res, WarnableValue(_, warnings)) =>
          res.copy(warnings = res.warnings ++ warnings)
      }
  }

  def defaultValue[A](c: Config, key: String, f: (Config, String) => A, default: A): A =
    try f(c, key)
    catch {
      case missing: ConfigException.Missing => default
    }

  def primWarning(message: String)(key: String, lineNumber: Int): String =
      s"Missing $key for primitive on line $lineNumber, $message"

  def argWarning(message: String)(key: String, lineNumber: Int): String =
    s"Argument on line $lineNumber has no $key, $message"

  def warnableValue[A](c: Config, key: String, message: (String, Int) => String, f: (Config, String) => A, default: A): WarnableValue[A] = {
      try {
        WarnableValue(f(c, key), Seq())
      } catch {
        case missing: ConfigException.Missing =>
          val lineNumber = c.origin.lineNumber
          WarnableValue(default, Seq(Warning(message(key, lineNumber), lineNumber)))
      }
  }

  def stringToType(s: String): TypeName =
    s.toLowerCase match {
      case ""               => WildcardType
      case "anything"       => WildcardType
      case "list"           => NetLogoList
      case "string"         => NetLogoString
      case "boolean"        => NetLogoBoolean
      case "number"         => NetLogoNumber
      case "patchset"       => Agentset(Patch)
      case "turtleset"      => Agentset(Turtle)
      case "linkset"        => Agentset(Link)
      case "turtle"         => Agent(Turtle)
      case "patch"          => Agent(Patch)
      case "symbol"         => Symbol
      case "code block"     => CodeBlock
      case "command block"  => CommandBlock
      case "command"        => CommandType
      case "reporter"       => ReporterType
      case "reporter block" => ReporterBlock
      case "reference"      => ReferenceType
      case "optional command block" => OptionalType
      case s if (s.startsWith("repeatable ")) => Repeatable(stringToType(s.stripPrefix("repeatable ")))
      case other => CustomType(other)
    }

  def getString(c: Config, s: String): String = c.getString(s)

  def getStringOption(c: Config, s: String): Option[String] = Option(c.getString(s))

  def foldArgs(parsedArgs: Seq[WarnableValue[NamedType]]): WarnableValue[Seq[NamedType]] = {
    parsedArgs.foldLeft(WarnableValue[Seq[NamedType]](Seq(), Seq())) {
      case (acc, theseArgs) => acc.merge[NamedType, Seq[NamedType]](theseArgs, _ :+ _)
    }
  }

  def parsePrimitive(extensionName: String)(c: Config): WarnableValue[Option[Primitive]] = {
    val nameOrError =
      warnableValue(c, "name", primWarning("excluding from results"), getStringOption _, None)

    def descriptionOrError(name: String) =
      warnableValue(c, "description",
        primWarning(s"adding empty description for primitive $name"), getString _, "")

    def primitiveType(name: String) =
      warnableValue(c, "type", primWarning(s"defaulting to command for primitive $name"), getString _, "command")
        .flatMap[PrimitiveType](
          procedureKind =>
            if (procedureKind == "command")
              WarnableValue(Command, Seq())
            else
              warnableValue(c, "returns", primWarning(s"assuming wildcard type for reporter $name"), getString _, "")
                .map(stringToType).map(Reporter(_)))

    val primSyntax = parsePrimSyntax(c)

    val tags: Seq[String] =
      defaultValue(c, "tags",
        (c: Config, k: String) => c.getAnyRefList(k), new java.util.ArrayList[AnyRef]()).asScala
        .collect { case s: String => s }

    def buildPrimitiveWithName(name: String): WarnableValue[Primitive] =
      descriptionOrError(name).flatMap(desc =>
          primitiveType(name).flatMap(primType =>
              primSyntax.map(syntax =>
                  Primitive(name, extensionName, primType, desc, syntax, tags))))

    nameOrError.flatMap((nameOpt: Option[String]) =>
        nameOpt
          .map(name => buildPrimitiveWithName(name).map(Some(_)))
          .getOrElse(WarnableValue[Option[Primitive]](None, nameOrError.warnings)))
  }

  def parsePrimSyntax(c: Config): WarnableValue[PrimSyntax] = {
    val infix = defaultValue[Boolean](c, "infix", _.getBoolean(_), false)

    val arguments =
      foldArgs(defaultValue[java.util.List[_ <: Config]](
        c, "arguments", _.getConfigList(_), new java.util.ArrayList[Config]()).asScala
          .map(parseNamedType)
          .toSeq)

    val altArguments: WarnableValue[Option[Seq[NamedType]]] =
      defaultValue[Option[java.util.List[_ <: Config]]](
        c, "alternateArguments", (c: Config, k: String) => Option(c.getConfigList(k)), None)
          .map(_.asScala.map(parseNamedType).toSeq)
          .map(foldArgs)
          .map(_.map(Option(_)))
          .getOrElse(WarnableValue(None, Seq()))

    val primArgs: WarnableValue[Seq[Seq[NamedType]]] =
      arguments.flatMap(args =>
          altArguments.map(altArgs =>
              if (args.isEmpty)
                Seq()
              else
                altArgs.map(aas => Seq(args, aas)).getOrElse(Seq(args))))

    primArgs.map(as => PrimSyntax(as, isInfix = infix))
  }

  def parseNamedType(c: Config): WarnableValue[NamedType] = {
    val argumentDescription = defaultValue[Option[String]](
      c, "name", getStringOption _, None)

    val atLocation = argumentDescription.map(" for argument name " + _).getOrElse("")
    warnableValue(c, "type", argWarning(s"assuming wildcard type$atLocation"), getString _, "anything")
      .map(stringToType)
      .map(argType =>
          argumentDescription
            .map(description => DescribedType(argType, description))
            .getOrElse(UnnamedType(argType)))
  }

  def parseConfig(config: Config): DocumentationConfig = {
    def getMap(c: Config, k: String): java.util.Map[String, Object] = c.getObject(k).unwrapped

    val additionalConfig =
      defaultValue[java.util.Map[String, Object]](config, "additionalVariables", getMap _, new java.util.HashMap[String, Object]())

    val tableOfContents: Map[String, String] = try {
      val tocConf = config.getObject("tableOfContents")
      tocConf.keySet.asScala.flatMap {
        case key =>
          defaultValue[Option[String]](tocConf.toConfig, key, getStringOption _, None)
            .map(value => (key, value))
      }.toMap
    } catch {
      case missing: ConfigException.Missing => Map[String, String]()
      case wrongType: ConfigException.WrongType => Map[String, String]()
    }
    new DocumentationConfig(
      config.getString("markdownTemplate"),
      config.getString("primTemplate"),
      tableOfContents,
      additionalConfig)
  }
}

