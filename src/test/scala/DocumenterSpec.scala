package org.nlogo.extensions

import org.scalatest.FunSpec

class DocumenterSpec extends FunSpec {
  describe("Documenter.renderPrimitive") {
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
        Documenter.renderPrimitive(basicPrim.withArgumentSet(Seq(UnnamedType(NetLogoList))).build, primTemplate))
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

      val docConfig = DocumentationConfig(markdownTemplate, primTemplate)
      assertResult(expectedDoc)(Documenter.documentAll(docConfig, Seq(basicPrim.build)))
    }
  }
}
