package org.nlogo.build

object PrimitiveBuilder {
  def empty = new PrimitiveBuilder()
}

class PrimitiveBuilder(
  name: String            = "command",
  extensionName: String   = "",
  description: String     = "",
  primType: PrimitiveType = Command,
  syntax: SyntaxBuilder   = SyntaxBuilder.empty,
  tags: Seq[String]       = Seq()) {

  def asReporter(returnType: TypeDescription): PrimitiveBuilder =
    new PrimitiveBuilder(name, extensionName, description, Reporter(returnType), syntax, tags)

  def asCommand: PrimitiveBuilder =
    new PrimitiveBuilder(name, extensionName, description, Command, syntax, tags)

  def tag(tag: String) =
    new PrimitiveBuilder(name, extensionName, description, primType, syntax, tags :+ tag)

  def name(s: String) =
    new PrimitiveBuilder(s, extensionName, description, primType, syntax, tags)

  def extension(extensionName: String) =
    new PrimitiveBuilder(name, extensionName, description, primType, syntax, tags)

  def syntax(f: SyntaxBuilder => SyntaxBuilder): PrimitiveBuilder =
    new PrimitiveBuilder(name, extensionName, description, primType, f(syntax), tags)

  def description(description: String): PrimitiveBuilder =
    new PrimitiveBuilder(name, extensionName, description, primType, syntax, tags)

  def build: Primitive =
    Primitive(name, extensionName, primType, description, syntax.build, tags)
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
