package org.nlogo.build

sealed trait AgentType {
  def agentName: String = "agent"
}

case object Turtle extends AgentType {
  override def agentName: String = "turtle"
}

case object Patch extends AgentType {
  override def agentName: String = "patch"
}

case object Link extends AgentType {
  override def agentName: String = "link"
}

case object Observer extends AgentType {
  override def agentName: String = "observer"
}

case class MultiAgent(agents: Seq[AgentType]) extends AgentType
object AllAgents extends MultiAgent(Seq(Observer, Turtle, Link, Patch))

sealed trait TypeDescription {
  def typeName = toString
}

case object NetLogoString extends TypeDescription {
  override val typeName = "string"
}
case object NetLogoBoolean extends TypeDescription {
  override val typeName = "boolean"
}
case object NetLogoNumber extends TypeDescription {
  override val typeName = "number"
}
case object NetLogoList extends TypeDescription {
  override val typeName = "list"
}
case class Agentset(agentType: AgentType) extends TypeDescription {
  override def typeName = agentType.agentName + "set"
}
case class Agent(agentType: AgentType) extends TypeDescription {
  override def typeName = agentType.agentName
}
case object Symbol extends TypeDescription {
  override def typeName = "variable"
}
case object CodeBlock extends TypeDescription {
  override def typeName = "code block"
}
case object CommandType extends TypeDescription {
  override def typeName = "anonymous command"
}
case object ReporterType extends TypeDescription {
  override def typeName = "anonymous reporter"
}
case object CommandBlock extends TypeDescription {
  override def typeName = "command block"
}
case class ReporterBlock(returnTypeDescription: TypeDescription) extends TypeDescription {
  override def typeName = "reporter block"
}
case object ReferenceType extends TypeDescription {
  override def typeName = "variable"
}
case object OptionalType extends TypeDescription {
  override def typeName = "optional command block"
}
case class Repeatable(repeatedType: TypeDescription) extends TypeDescription
case object WildcardType extends TypeDescription {
  override def typeName = "anything"
}
case class CustomType(customName: String) extends TypeDescription {
  override def typeName = customName
}

sealed trait NamedType {
  def description: String
  def typeDescription: TypeDescription
}

case class UnnamedType(typeDescription: TypeDescription) extends NamedType {
  override def description = typeDescription.typeName
}

case class DescribedType(typeDescription: TypeDescription, description: String) extends NamedType

sealed trait PrimitiveType

case class Reporter(returnType: TypeDescription) extends PrimitiveType
case object Command extends PrimitiveType

case class PrimSyntax(arguments: Seq[Seq[NamedType]], agentContext: AgentType = AllAgents, isInfix: Boolean = false)

case class Primitive(
  name: String,
  extensionName: String,
  primitiveType: PrimitiveType,
  description: String,
  syntax: PrimSyntax,
  tags: Seq[String] = Seq()) {
    def arguments = syntax.arguments
    def fullName = if (extensionName == "") name else s"$extensionName:$name"
  }
