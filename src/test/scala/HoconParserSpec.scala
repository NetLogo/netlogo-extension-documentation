package org.nlogo.extensions

import org.scalatest.FunSpec

class HoconParserSpec extends FunSpec {
  def parse(s: String): Seq[Primitive] = HoconParser(s).primitives
  def parseWarnings(s: String): Seq[Warning] = HoconParser(s).warnings

  def assertContainsPrimitive(s: String, p: Primitive) =
    assert(parse(s).contains(p))

  def assertContainsWarning(s: String, warning: Warning) =
    assert(parseWarnings(s).contains(warning))

  describe("Parser") {
    it("errors on empty text") {
      intercept[Exception] {
        HoconParser("")
      }
    }
    it("parses empty primitives list") {
      assertResult(Seq[Primitive]())(parse("primitives: []"))
    }
    it("parses simple primitive list") {
      val simplePrimitiveList =
        """|primitives: [
           |  {
           |    name: "do-something",
           |    description: "does something"
           |  }
           |]""".stripMargin
      assertContainsPrimitive(simplePrimitiveList,
        Primitive("do-something", Command, description = TextString("does something"), arguments = Seq(Seq())))
    }
    it("warns when name is missing") {
      val missingNameList =
        "primitives: [ \n { description : \"does something\" } \n ]"
      assertContainsWarning(missingNameList, Warning("Missing name for primitive on line 2, excluding from results", 2))
      assertResult(Seq())(parse(missingNameList))
    }

    it("warns when description is missing") {
      val missingDescription =
        "primitives: [ \n { name : \"do-something\" } \n ]"
      assertContainsWarning(missingDescription, Warning("Missing description for primitive on line 2, adding empty description", 2))
      assertContainsPrimitive(missingDescription,
        Primitive("do-something", Command, description = TextString(""), arguments = Seq(Seq())))
    }

    it("returns a reporter with arguments") {
      val reporterDescription =
        """primitives: [ { name : "a-reporter", type: "reporter", returns: "list" } ]"""
      assertContainsPrimitive(reporterDescription,
        Primitive("a-reporter", Reporter(NetLogoList), description = TextString(""), arguments = Seq(Seq())))
    }

    it("warns if a reporter has no named type") {
      val missingType =
        """primitives: [ { name : "do-something", description: "", type: reporter } ]"""
      assertContainsWarning(missingType,
        Warning("Missing returns for primitive on line 1, assuming wildcard type", 1))
      assertContainsPrimitive(missingType,
        Primitive("do-something", Reporter(WildcardType), description = TextString(""), arguments = Seq(Seq())))
    }

    it("returns primitives that return agentsets of a specific type") {
      val agentSetReturn =
        """primitives: [ { name : "blue-turtles", description: "", type: reporter, returns: "turtleset" } ]"""
      assertContainsPrimitive(agentSetReturn,
        Primitive("blue-turtles", Reporter(Agentset(Turtle)), description = TextString(""), arguments = Seq(Seq())))
    }
  }
}
