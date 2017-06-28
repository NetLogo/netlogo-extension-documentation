package org.nlogo.build

import org.scalatest.FunSpec

import java.nio.file.Files

class DocumenterSpec extends FunSpec {
  describe("Documenter.renderPrimitive") {
    val dummyPath = new java.io.File(".").toPath

    val basicPrim =
      PrimitiveBuilder.empty.name("foo").description("does stuff")
    val primTemplate =
      """|### {{name}}
         |
         |```NetLogo
         |{{#examples}}{{name}}{{#args}} *{{typeName.name}}*{{/args}}{{/examples}}
         |```
         |
         |{{description}}""".stripMargin

    it("renders primitives into markdown") {
      assertResult("""|### foo
                      |
                      |```NetLogo
                      |foo
                      |```
                      |
                      |does stuff""".stripMargin)(
        Documenter.renderPrimitive(basicPrim.build, primTemplate))
    }

    it("renders primitives with arguments into markdown") {
      assertResult("""|### foo
                      |
                      |```NetLogo
                      |foo *list*
                      |```
                      |
                      |does stuff""".stripMargin)(
        Documenter.renderPrimitive(basicPrim.syntax(_.withArgumentSet(Seq(UnnamedType(NetLogoList)))).build, primTemplate))
    }

    it("makes _name_ and _fullName_ available for lowercase-only use") {
      val basicPrim =
        PrimitiveBuilder.empty.name("fooBar").description("does stuff")
      val primTemplate =
        """|<div id="{{_name_}}">
           |{{#examples}}
           |<tt>{{_name_}}</tt>
           |{{/examples}}
           |</div>""".stripMargin
      val renderedPrim = Documenter.renderPrimitive(basicPrim.build, primTemplate)
      assertResult(
        """|<div id="foobar">
           |<tt>foobar</tt>
           |</div>""".stripMargin)(renderedPrim)
    }

    it("renders infix primitives") {
      val primTemplate = """{{#isInfix}}{{#examples}}{{leftArg.name}} {{name}}{{#rightArgs}} {{name}}{{/rightArgs}}{{/examples}}{{/isInfix}}"""
      val infixPrim = basicPrim
        .syntax(_.infix)
        .syntax(_.withArgumentSet(Seq(DescribedType(NetLogoList, "a"), DescribedType(NetLogoList, "b"))))
        .build
      val expectedResult = """a foo b"""
      assertResult(expectedResult)(Documenter.renderPrimitive(infixPrim, primTemplate))
    }

    it("renders whole documents according to the doc config") {
      val expectedDoc =
        """|about this
           |
           |### foo
           |
           |```NetLogo
           |foo
           |```
           |
           |does stuff
           |
           |license stuff""".stripMargin

      val markdownTemplate =
        """|about this
           |
           |{{#allPrimitives}}
           |{{{.}}}
           |{{/allPrimitives}}
           |
           |license stuff""".stripMargin

      val docConfig = DocumentationConfig(markdownTemplate, primTemplate, Map())
      assertResult(expectedDoc)(Documenter.documentAll(docConfig, Seq(basicPrim.build), dummyPath))
    }

    it("renders the doc using the primTemplate partial") {
      val expectedDoc =
        """|about this
           |
           |### foo
           |
           |```NetLogo
           |foo
           |```
           |
           |does stuff
           |
           |license stuff""".stripMargin

      val markdownTemplate =
        """|about this
           |
           |{{#primitives}}
           |{{> primTemplate}}
           |{{/primitives}}
           |
           |license stuff""".stripMargin

      val docConfig = DocumentationConfig(markdownTemplate, primTemplate, Map())
      assertResult(expectedDoc)(Documenter.documentAll(docConfig, Seq(basicPrim.build), dummyPath))
    }

    it("renders whole documents with included files") {
      val tempFile = Files.createTempFile("testInclude", ".txt")
      Files.write(tempFile, "included text {{a}}".getBytes)
      val docConfig = DocumentationConfig(s"not-included text {{#include}}${tempFile.getName(tempFile.getNameCount - 1)}{{/include}}", "", Map(), Map("a" -> "here"))
      assertResult("not-included text included text here")(Documenter.documentAll(docConfig, Seq(basicPrim.build), tempFile.getParent))
    }

    it("renders whole documents with included files as partials") {
      val tempFile = Files.createTempFile("testInclude", ".txt")
      Files.write(tempFile, "included text {{a}}".getBytes)
      val docConfig = DocumentationConfig(s"not-included text {{> ${tempFile.getName(tempFile.getNameCount - 1)}}}", "", Map(), Map("a" -> "here"))
      assertResult("not-included text included text here")(Documenter.documentAll(docConfig, Seq(basicPrim.build), tempFile.getParent))
    }

    it("makes additional config variables available to markdownTemplate") {
      val docConfig = DocumentationConfig("{{a}} {{b}}", "", Map(), Map("a" -> "1", "b" -> "2"))
      assertResult("1 2")(Documenter.documentAll(docConfig, Seq(), dummyPath))
    }

    it("makes additional config variables available to primitive templates") {
      val docConfig = DocumentationConfig("{{#primitives}}{{> primTemplate}}{{/primitives}}", "{{a}}", Map(), Map("a" -> "here"))
      assertResult("here")(Documenter.documentAll(docConfig, Seq(basicPrim.build), dummyPath))
    }

    it("makes data available for a table of contents") {
      val docTemplate =
        "{{#contents}}{{fullCategoryName}} {{#prims}}{{name}} {{/prims}}{{/contents}}"
      val docConfig = DocumentationConfig(docTemplate, "",
        Map("tag" -> "Full Name"), Map("a" -> "1", "b" -> "2"))
      val prim = basicPrim.tag("tag").build
      assertResult("Full Name foo ")(Documenter.documentAll(docConfig, Seq(prim), dummyPath))
    }

    it("makes data available when there are no categories") {
      val prim = basicPrim.tag("tag").build
      val docTemplate =
        "{{#contents}}{{fullCategoryName}} {{#prims}}{{name}} {{/prims}}{{/contents}}"
      val docConfig = DocumentationConfig(docTemplate, "", Map(), Map())
      assertResult(" foo ")(Documenter.documentAll(docConfig, Seq(prim), dummyPath))
    }
  }
}
