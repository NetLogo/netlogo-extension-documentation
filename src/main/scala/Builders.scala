package org.nlogo.build

object PrimitiveBuilder {
  def empty = new PrimitiveBuilder()
}

class PrimitiveBuilder(
  name: String                   = "command",
  description: String            = "",
  arguments: Seq[Seq[NamedType]] = Seq(),
  primType: PrimitiveType        = Command,
  agentContext: AgentType        = AllAgents) {

  def asReporter(returnType: TypeDescription): PrimitiveBuilder =
    new PrimitiveBuilder(name, description, arguments, Reporter(returnType), agentContext)

  def asCommand: PrimitiveBuilder =
    new PrimitiveBuilder(name, description, arguments, Command, agentContext)

  def name(s: String) =
    new PrimitiveBuilder(s, description, arguments, primType, agentContext)

  def withArgumentSet(argSet: Seq[NamedType]): PrimitiveBuilder =
    new PrimitiveBuilder(name, description, arguments :+ argSet, primType, agentContext)

  def description(description: String): PrimitiveBuilder =
    new PrimitiveBuilder(name, description, arguments, primType, agentContext)

  def build: Primitive =
    Primitive(name, primType, description, arguments, agentContext)
}
