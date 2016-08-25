package org.nlogo.build

import org.scalatest.FunSpec

import HoconParser._

class HoconParserSpec extends FunSpec {
  def parse(s: String): Seq[Primitive] = HoconParser.parsePrimitives(parseConfigText(s)).primitives
  def parseWarnings(s: String): Seq[Warning] = HoconParser.parsePrimitives(parseConfigText(s)).warnings

  def assertContainsPrimitive(s: String, p: Primitive) =
    assert(parse(s).contains(p))

  def assertContainsWarning(s: String, warning: Warning) =
    assert(parseWarnings(s).contains(warning))

  val commandBuilder = PrimitiveBuilder.empty.name("do-something")
  val reporterBuilder = commandBuilder.name("a-reporter")

  def kv(fields: Seq[(String, String)]): String =
    fields.map {
      case (k, v) =>
        val finalV = if (v.startsWith("[") || v.startsWith("{") || v == "true" || v == "false") v else s""""$v""""
        s""""$k" : $finalV"""
      }.mkString("{ ", ", ", " }")

  def args(arguments: Seq[String]): (String, String) =
    "arguments" -> arguments.mkString("[ ", ",\n", " ]")

  def altArgs(arguments: Seq[String]): (String, String) =
    "alternateArguments" -> arguments.mkString("[ ", ",\n", " ]")

  def primitives(primitiveStrings: String*): String =
    primitiveStrings.mkString("primitives: [\n", ",\n", "\n]")

  val baseReporter: Seq[(String, String)] = Seq("name" -> "a-reporter", "type" -> "reporter")
  val baseCommand: Seq[(String, String)] = Seq("name" -> "do-something")

  describe("HoconParser.parsePrimitives") {
    it("errors on empty text") {
      intercept[Exception] { HoconParser.parsePrimitives(parseConfigText("")) }
    }

    it("parses empty primitives list") {
      assertResult(Seq[Primitive]())(parse(primitives()))
    }

    it("parses a single primitive") {
      val simplePrimitiveList =
        primitives(kv(baseCommand :+ "description" -> "does something"))
      val expected = commandBuilder.description("does something")
      assertContainsPrimitive(simplePrimitiveList, expected.build)
    }

    it("parses multiple primitives") {
      val simplePrimitiveList = primitives(kv(baseCommand), kv(baseReporter))
      assertContainsPrimitive(simplePrimitiveList, commandBuilder.build)
      assertContainsPrimitive(simplePrimitiveList, reporterBuilder.asReporter(WildcardType).build)
    }

    it("warns when name is missing") {
      val missingNameList = primitives(kv(Seq("description" -> "does something")))
      assertContainsWarning(missingNameList,
        Warning("Missing name for primitive on line 2, excluding from results", 2))
      assertResult(Seq())(parse(missingNameList))
    }

    it("warns when description is missing") {
      val missingDescription = primitives(kv(baseCommand))
      assertContainsWarning(missingDescription,
        Warning("Missing description for primitive on line 2, adding empty description", 2))
      assertContainsPrimitive(missingDescription, commandBuilder.description("").build)
    }

    it("returns a reporter with return type") {
      val reporterDescription =
        primitives(kv(baseReporter :+ "returns" -> "list"))
      assertContainsPrimitive(reporterDescription, reporterBuilder.asReporter(NetLogoList).build)
    }

    it("warns if a reporter has no named type") {
      val missingType = primitives(kv(baseReporter))
      assertContainsWarning(missingType,
        Warning("Missing returns for primitive on line 2, assuming wildcard type", 2))
      assertContainsPrimitive(missingType, reporterBuilder.asReporter(WildcardType).build)
    }

    it("returns primitives that return agentsets of a specific type") {
      val agentSetReturn = primitives(kv(baseReporter :+ "returns" -> "turtleset"))
      assertContainsPrimitive(agentSetReturn, reporterBuilder.asReporter(Agentset(Turtle)).build)
    }

    it("allows specifying un-named argument") {
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq(kv(Seq("type" -> "list"))))))
      assertContainsPrimitive(argumentPrim, commandBuilder.syntax(_.withArgumentSet(Seq(UnnamedType(NetLogoList)))).build)
    }

    val agentColorList = kv(Seq("type" -> "list", "description" -> "agent colors"))

    it("allows specifying named arguments") {
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq(agentColorList))))
      assertContainsPrimitive(argumentPrim, commandBuilder.syntax(_.withArgumentSet(Seq(DescribedType(NetLogoList, "agent colors")))).build)
    }

    it("doesn't blow up when arguments improperly specified") {
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq("{}"))))
      assertContainsWarning(argumentPrim, Warning("Argument on line 2 has no type, assuming wildcard type", 2))
      assertContainsPrimitive(argumentPrim, commandBuilder.syntax(_.withArgumentSet(Seq(UnnamedType(WildcardType)))).build)
    }

    it("allows specifying multiple arguments") {
      val argument2 = kv(Seq("type" -> "patch"))
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq(agentColorList, argument2))))
      assertContainsPrimitive(argumentPrim,
        commandBuilder.syntax(_.withArgumentSet(Seq(DescribedType(NetLogoList, "agent colors"), UnnamedType(Agent(Patch))))).build)
    }

    it("allows specifying alternate argument lists") {
      val multiArgPrim = primitives(kv(baseCommand :+
        args(Seq(agentColorList)) :+
        altArgs(Seq(kv(Seq("type" -> "turtleset"))))))
      assertContainsPrimitive(multiArgPrim,
        commandBuilder
          .syntax(_.withArgumentSet(Seq(DescribedType(NetLogoList, "agent colors"))))
          .syntax(_.withArgumentSet(Seq(UnnamedType(Agentset(Turtle))))).build)
    }

    it("prefixes the primitive name with the extension name, if present") {
      val extensionPrims = primitives(kv(baseCommand)) + "\nextensionName: bar"
      assertContainsPrimitive(extensionPrims, commandBuilder.extension("bar").build)
    }

    it("makes infix data available") {
      val infixPrim = primitives(kv(baseCommand :+ ("infix" -> "true")))
      assertContainsPrimitive(infixPrim, commandBuilder.syntax(_.infix).build)
    }

    it("parses tags") {
      val taggedPrim = primitives(kv(baseCommand :+ ("tags" -> "[ \"a-tag\" ]")))
      assertContainsPrimitive(taggedPrim, commandBuilder.tag("a-tag").build)
    }
  }

  describe("HoconParser.parseConfiguration") {
    it("returns document configuration") {
        val configText =
          s"""|markdownTemplate: "{{allPrimitives}}"
              |primitives: [
              | {
              | name: foo
              | description: "does stuff"
              | }
              |],
              |primTemplate: "{{name}}"""".stripMargin
       val parsedConfig = HoconParser.parseConfig(HoconParser.parseConfigText(configText))
       assert(parsedConfig.primTemplate == "{{name}}")
       assert(parsedConfig.markdownTemplate == "{{allPrimitives}}")
       assert(parsedConfig.tableOfContents == Map())
    }

    it("parses tableOfContents") {
      val withTableOfContents =
        """|markdownTemplate: ""
           |primitives: [],
           |primTemplate: "",
           |tableOfContents: {
           | "foo": "bar"
           |}""".stripMargin
      val parsedConfig = HoconParser.parseConfig(HoconParser.parseConfigText(withTableOfContents))
      assert(parsedConfig.tableOfContents == Map("foo" -> "bar"))
    }
  }
}
