package org.nlogo.extensions

import org.scalatest.FunSpec

class HoconParserSpec extends FunSpec {
  def parse(s: String): Seq[Primitive] = HoconParser(s).primitives
  def parseWarnings(s: String): Seq[Warning] = HoconParser(s).warnings

  def assertContainsPrimitive(s: String, p: Primitive) =
    assert(parse(s).contains(p))

  def assertContainsWarning(s: String, warning: Warning) =
    assert(parseWarnings(s).contains(warning))

  val commandBuilder = PrimitiveBuilder.empty.name("do-something")
  val reporterBuilder = commandBuilder.name("a-reporter")

  def kv(fields: Seq[(String, String)]): String =
    fields.map {
      case (k, v) =>
        val finalV = if (v.startsWith("[") || v.startsWith("{")) v else s""""$v""""
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

  describe("Parser") {
    it("errors on empty text") {
      intercept[Exception] {
        HoconParser("")
      }
    }

    it("parses empty primitives list") {
      assertResult(Seq[Primitive]())(parse(primitives()))
    }

    it("parses simple primitive list") {
      val simplePrimitiveList =
        primitives(kv(baseCommand :+ "description" -> "does something"))
      val expected = commandBuilder.description("does something")
      assertContainsPrimitive(simplePrimitiveList, expected.build)
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
      assertContainsPrimitive(argumentPrim, commandBuilder.withArgumentSet(Seq(UnnamedType(NetLogoList))).build)
    }

    val agentColorList = kv(Seq("type" -> "list", "description" -> "agent colors"))

    it("allows specifying named arguments") {
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq(agentColorList))))
      assertContainsPrimitive(argumentPrim, commandBuilder.withArgumentSet(Seq(DescribedType(NetLogoList, "agent colors"))).build)
    }

    it("doesn't blow up when arguments improperly specified") {
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq("{}"))))
      assertContainsWarning(argumentPrim, Warning("Argument on line 2 has no type, assuming wildcard type", 2))
      assertContainsPrimitive(argumentPrim, commandBuilder.withArgumentSet(Seq(UnnamedType(WildcardType))).build)
    }

    it("allows specifying multiple arguments") {
      val argument2 = kv(Seq("type" -> "patch"))
      val argumentPrim = primitives(kv(baseCommand :+ args(Seq(agentColorList, argument2))))
      assertContainsPrimitive(argumentPrim,
        commandBuilder.withArgumentSet(Seq(DescribedType(NetLogoList, "agent colors"), UnnamedType(Agent(Patch)))).build)
    }

    it("allows specifying alternate argument lists") {
      val multiArgPrim = primitives(kv(baseCommand :+
        args(Seq(agentColorList)) :+
        altArgs(Seq(kv(Seq("type" -> "turtleset"))))))
      assertContainsPrimitive(multiArgPrim,
        commandBuilder
          .withArgumentSet(Seq(DescribedType(NetLogoList, "agent colors")))
          .withArgumentSet(Seq(UnnamedType(Agentset(Turtle)))).build)
    }
  }
}
