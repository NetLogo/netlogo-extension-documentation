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
         |{{#examples}}{{name}}{{#args}} *{{typeDescription.typeName}}*{{/args}}{{/examples}}
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

    it("renders whole documents with included files") {
      val tempFile = Files.createTempFile("testInclude", ".txt")
      Files.write(tempFile, "included text".getBytes)
      val docConfig = DocumentationConfig(s"not-included text {{#include}}${tempFile.getName(tempFile.getNameCount - 1)}{{/include}}", "", Map())
      assertResult("not-included text included text")(Documenter.documentAll(docConfig, Seq(basicPrim.build), tempFile.getParent))
    }

    it("makes additional config variables available to markdownTemplate") {
      val docConfig = DocumentationConfig("{{a}} {{b}}", "", Map(), Map("a" -> "1", "b" -> "2"))
      assertResult("1 2")(Documenter.documentAll(docConfig, Seq(), dummyPath))
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
