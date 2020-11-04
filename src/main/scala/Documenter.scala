package org.nlogo.build

import java.util.{ Map => JMap }
import java.io.{ IOException, Reader, StringReader, StringWriter, Writer }
import java.nio.file.{ Files, Path, Paths }

import com.github.mustachejava._

import scala.collection.JavaConverters._

object Documenter {
  trait PrimExample {
    def primitive: Primitive
    def args: java.util.List[NamedType]
    def name = primitive.fullName
    def _name_ = primitive.fullName.toLowerCase
  }

  class PrefixPrimExample(val primitive: Primitive, argSet: Seq[NamedType], val isOptional: Boolean) extends PrimExample {
    def args: java.util.List[NamedType] = argSet.asJava
  }

  class InfixPrimExample(val primitive: Primitive, argSet: Seq[NamedType]) extends PrimExample {
    def leftArg:   NamedType                 = argSet.head
    def rightArgs: java.util.List[NamedType] = argSet.tail.asJava
    def args:      java.util.List[NamedType] = argSet.asJava
  }

  class MustachePrimWrapper(val primitive: Primitive, additional: Map[String, AnyRef]) {
    def name             = primitive.fullName
    def _name_           = primitive.fullName.toLowerCase
    lazy val description = renderMustache(primitive.description, additional.asJava)
    def isInfix          = primitive.syntax.isInfix
    def examples: java.util.List[_ <: PrimExample] =
      if (primitive.arguments.isEmpty)
        Seq(new PrefixPrimExample(primitive, Seq(), false)).asJava
      else if (primitive.syntax.isInfix)
        primitive.arguments.map(argSet => new InfixPrimExample(primitive, argSet)).asJava
      else
        primitive.arguments.zipWithIndex.map { case (argSet, index) =>
          new PrefixPrimExample(primitive, argSet, index > 0 && primitive.syntax.areAltArgsOptional)
        }.asJava
    def toMap: JMap[String, AnyRef] =
      (Map(
        "name"        -> name,
        "_name_"      -> _name_,
        "description" -> description,
        "isInfix"     -> Boolean.box(isInfix),
        "examples"    -> examples) ++ additional).asJava

  }

  class ContentSection(val fullCategoryName: String, val shortCategoryName: String, primitives: Seq[Primitive]) {
    val prims = primitives.map(new MustachePrimWrapper(_, Map())).asJava
  }

  def renderPrimitive(prim: Primitive, mustacheTemplate: String): String = {
    renderMustache(mustacheTemplate, new MustachePrimWrapper(prim, Map()))
  }

  private def renderMustache(template: String, scope: AnyRef): String = {
    val mr = new MustacheResolver {
      override def getReader(resourceName: String): Reader =
        new StringReader(template)
    }
    val mf = new DefaultMustacheFactory(mr)
    val stache = mf.compile("primitives")
    val out = new StringWriter()
    stache.execute(out, scope).flush()
    out.toString
  }

  def contentSection(prims: Seq[Primitive])(shortName: String, fullName: String): ContentSection = {
    new ContentSection(fullName, shortName, prims.filter(_.tags.contains(shortName)))
  }

  def documentAll(docConfig: DocumentationConfig, prims: Seq[Primitive], basePath: Path): String = {
    val mr = new MustacheResolver {
      override def getReader(resourceName: String): Reader = {
        val s =
          if (resourceName == "document")
            docConfig.markdownTemplate
          else if (resourceName == "primTemplate")
            docConfig.primTemplate
          else
            try {
              Files.readAllLines(basePath.resolve(resourceName)).asScala.mkString("\n")
            } catch {
              case io: IOException => ""
            }
        new StringReader(s)
      }
    }

    val mf = new DefaultMustacheFactory(mr)
    val stache = mf.compile("document")
    val out = new StringWriter()

    val userSpecifiedContents = docConfig.tableOfContents.map((contentSection(prims) _).tupled).toSeq.asJava

    val tableOfContents =
      if (userSpecifiedContents.isEmpty) Seq(new ContentSection("", "", prims)).asJava else userSpecifiedContents

    val variables = Map(
      "primitives"    -> prims.map(new MustachePrimWrapper(_, docConfig.additionalConfig).toMap).asJava,
      "allPrimitives" -> new AllPrimitivesAdapter(),
      "contents"      -> tableOfContents,
      "include"       -> new IncludeFile(basePath)) ++
      docConfig.additionalConfig
    stache.execute(out, variables.asJava).flush()
    out.toString
  }

  class IncludeFile(basePath: Path) extends TemplateFunction {
    override def apply(filename: String): String = {
      Files.readAllLines(basePath.resolve(filename)).asScala.mkString("\n")
    }
  }

  class AllPrimitivesAdapter extends TemplateFunction {
    override def apply(content: String): String = {
      val mr = new MustacheResolver {
        override def getReader(resourceName: String): Reader =
          new StringReader(content)
      }
      val mf = new DefaultMustacheFactory(mr)
      val stache = mf.compile("document")
      val out = new StringWriter()
      val evaluated = stache.execute(out, "{{> primTemplate}}")
      s"""|{{#primitives}}${evaluated}
          |{{/primitives}}""".stripMargin
    }
  }
}
