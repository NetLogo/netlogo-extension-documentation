package org.nlogo.build

import java.io.{ Reader, StringReader, StringWriter }
import java.nio.file.{ Files, Path, Paths }

import com.github.mustachejava._
import scala.collection.JavaConverters._

object Documenter {
  trait PrimExample {
    def primitive: Primitive
    def args: java.util.List[NamedType]
  }

  class PrefixPrimExample(val primitive: Primitive, argSet: Seq[NamedType]) extends PrimExample {
    def args: java.util.List[NamedType] = argSet.asJava
  }

  class InfixPrimExample(val primitive: Primitive, argSet: Seq[NamedType]) extends PrimExample{
    def leftArg:   NamedType                 = argSet.head
    def rightArgs: java.util.List[NamedType] = argSet.tail.asJava
    def args:      java.util.List[NamedType] = argSet.asJava
  }

  class MustachePrimWrapper(val primitive: Primitive) {
    def name            = primitive.fullName
    def description     = primitive.description
    def isInfix         = primitive.syntax.isInfix
    def examples: java.util.List[_ <: PrimExample] =
      if (primitive.arguments.isEmpty)
        Seq(new PrefixPrimExample(primitive, Seq())).asJava
      else if (primitive.syntax.isInfix)
        primitive.arguments.map(argSet => new InfixPrimExample(primitive, argSet)).asJava
      else
        primitive.arguments.map(argSet => new PrefixPrimExample(primitive, argSet)).asJava
  }

  class ContentSection(val fullCategoryName: String, val shortCategoryName: String, primitives: Seq[Primitive]) {
    val prims = primitives.map(new MustachePrimWrapper(_)).asJava
  }

  def renderPrimitive(prim: Primitive, mustacheTemplate: String): String = {
    val mr = new MustacheResolver {
      override def getReader(resourceName: String): Reader =
        new StringReader(mustacheTemplate)
    }
    val mf = new DefaultMustacheFactory(mr)
    val stache = mf.compile("primitives")
    val out = new StringWriter()
    stache.execute(out, new MustachePrimWrapper(prim)).flush()
    out.toString
  }

  def contentSection(prims: Seq[Primitive])(shortName: String, fullName: String): ContentSection = {
    new ContentSection(fullName, shortName, prims.filter(_.tags.contains(shortName)))
  }

  def documentAll(docConfig: DocumentationConfig, prims: Seq[Primitive], basePath: Path): String = {
    val renderedPrims = prims.map(renderPrimitive(_, docConfig.primTemplate))

    val mr = new MustacheResolver {
      override def getReader(resourceName: String): Reader =
        new StringReader(docConfig.markdownTemplate)
    }
    val mf = new DefaultMustacheFactory(mr)
    val stache = mf.compile("document")
    val out = new StringWriter()

    val userSpecifiedContents = docConfig.tableOfContents.map((contentSection(prims) _).tupled).toSeq.asJava

    val tableOfContents =
      if (userSpecifiedContents.isEmpty) Seq(new ContentSection("", "", prims)).asJava else userSpecifiedContents

    val variables = Map(
      "allPrimitives" -> renderedPrims.asJava,
      "contents"      -> tableOfContents,
      "include"       -> new IncludeFile(basePath)) ++
      docConfig.additionalConfig
    stache.execute(out, variables.asJava).flush()
    out.toString
  }

  class IncludeFile(basePath: Path) extends java.util.function.Function[String, String] {
    override def apply(filename: String): String = {
      Files.readAllLines(basePath.resolve(filename)).asScala.mkString("\n")
    }
  }
}
