package org.nlogo.build

object PrimitiveBuilder {
  def empty = new PrimitiveBuilder()
}

class PrimitiveBuilder(
  name: String            = "command",
  description: String     = "",
  primType: PrimitiveType = Command,
  syntax: SyntaxBuilder   = SyntaxBuilder.empty,
  tags: Seq[String]       = Seq()) {

  def asReporter(returnType: TypeDescription): PrimitiveBuilder =
    new PrimitiveBuilder(name, description, Reporter(returnType), syntax, tags)

  def asCommand: PrimitiveBuilder =
    new PrimitiveBuilder(name, description, Command, syntax, tags)

  def tag(tag: String) =
    new PrimitiveBuilder(name, description, primType, syntax, tags :+ tag)

  def name(s: String) =
    new PrimitiveBuilder(s, description, primType, syntax, tags)

  def syntax(f: SyntaxBuilder => SyntaxBuilder): PrimitiveBuilder =
    new PrimitiveBuilder(name, description, primType, f(syntax), tags)

  def description(description: String): PrimitiveBuilder =
    new PrimitiveBuilder(name, description, primType, syntax, tags)

  def build: Primitive =
    Primitive(name, primType, description, syntax.build, tags)
}

object SyntaxBuilder {
  def empty = new SyntaxBuilder()
}

class SyntaxBuilder(
  arguments: Seq[Seq[NamedType]] = Seq(),
  agentContext: AgentType = AllAgents,
  isInfix: Boolean = false) {
  def withArgumentSet(argSet: Seq[NamedType]): SyntaxBuilder =
    new SyntaxBuilder(arguments :+ argSet, agentContext, isInfix)

  def infix: SyntaxBuilder =
    new SyntaxBuilder(arguments, agentContext, true)

  def build: PrimSyntax = PrimSyntax(arguments, agentContext, isInfix)
}
