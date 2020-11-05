package org.nlogo.build

object ArgumentPlaceholder {
  var spaceReplacement = '-'
}

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

sealed trait TypeName {
  def name = toString
}

case object NetLogoString extends TypeName {
  override val name = "string"
}
case object NetLogoBoolean extends TypeName {
  override val name = "boolean"
}
case object NetLogoNumber extends TypeName {
  override val name = "number"
}
case object NetLogoList extends TypeName {
  override val name = "list"
}
case class Agentset(agentType: AgentType) extends TypeName {
  override def name = agentType.agentName + "set"
}
case class Agent(agentType: AgentType) extends TypeName {
  override def name = agentType.agentName
}
case object Symbol extends TypeName {
  override def name = "variable"
}
case object CodeBlock extends TypeName {
  override def name = "code block"
}
case object CommandType extends TypeName {
  override def name = "anonymous command"
}
case object ReporterType extends TypeName {
  override def name = "anonymous reporter"
}
case object CommandBlock extends TypeName {
  override def name = "command block"
}
case object ReporterBlock extends TypeName {
  override def name = "reporter block"
}
case object ReferenceType extends TypeName {
  override def name = "variable"
}
case object OptionalType extends TypeName {
  override def name = "optional command block"
}
case class Repeatable(repeatedType: TypeName) extends TypeName {
  override def name = s"${repeatedType.name}..."
}
case object WildcardType extends TypeName {
  override def name = "anything"
}
case class CustomType(customName: String) extends TypeName {
  override def name = customName
}

sealed trait NamedType {
  def name: String
  def typeName: TypeName
  def argumentPlaceholder: String = name.replace(' ', ArgumentPlaceholder.spaceReplacement)
}

case class UnnamedType(typeName: TypeName) extends NamedType {
  override def name = typeName.name
}

case class DescribedType(typeName: TypeName, name: String) extends NamedType

sealed trait PrimitiveType

case class Reporter(returnType: TypeName) extends PrimitiveType
case object Command extends PrimitiveType

case class PrimSyntax(arguments: Seq[Seq[NamedType]], agentContext: AgentType = AllAgents, isInfix: Boolean = false) {
  def areAltArgsOptional: Boolean =
    arguments.length > 1 && arguments(0).length != arguments(1).length
}

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
