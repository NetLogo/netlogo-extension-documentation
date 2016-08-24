package org.nlogo.extensions

import java.io.{ Reader, StringReader, StringWriter }

import com.github.mustachejava._
import scala.collection.JavaConverters._

object Documenter {
  class PrimExample(prim: Primitive, argSet: Seq[NamedType]) {
    def args: java.util.List[NamedType] = argSet.asJava
  }

  class MustachePrimWrapper(prim: Primitive) {
    def name            = prim.fullName
    def description     = prim.description
    def examples: java.util.List[PrimExample] =
      if (prim.arguments.isEmpty)
        Seq(new PrimExample(prim, Seq())).asJava
      else
        prim.arguments.map(argSet => new PrimExample(prim, argSet)).asJava
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

  def documentAll(docConfig: DocumentationConfig, prims: Seq[Primitive]): String = {
    val renderedPrims = prims.map(renderPrimitive(_, docConfig.primTemplate))

    val mr = new MustacheResolver {
      override def getReader(resourceName: String): Reader =
        new StringReader(docConfig.markdownTemplate)
    }
    val mf = new DefaultMustacheFactory(mr)
    val stache = mf.compile("document")
    val out = new StringWriter()
    stache.execute(out, Map("allPrimitives" -> renderedPrims.asJava).asJava).flush()
    out.toString
  }
}
