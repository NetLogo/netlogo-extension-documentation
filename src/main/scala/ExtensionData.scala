package org.nlogo.extensions

sealed trait FormattedString {
  def toHtml: String
  def toMarkdown: String
  def toText: String
}

case class TextString(s: String) extends FormattedString {
  def toHtml: String = s
  def toMarkdown: String = s
  def toText: String = s
}

sealed trait AgentType
case object Turtle extends AgentType
case object Patch extends AgentType
case object Link extends AgentType
case object Observer extends AgentType
case class MultiAgent(agents: Seq[AgentType]) extends AgentType
case object Nobody extends AgentType

sealed trait TypeDescription

case object NetLogoString extends TypeDescription
case object NetLogoBoolean extends TypeDescription
case object NetLogoNumber extends TypeDescription
case object NetLogoList extends TypeDescription
case class Agentset(agentType: AgentType) extends TypeDescription
case class Agent(agentType: AgentType) extends TypeDescription
case object Symbol extends TypeDescription
case object CodeBlock extends TypeDescription
case class AnonymousProcedure(
  procedureType:    PrimitiveType,
  acceptsArguments: Seq[NamedType]) extends TypeDescription
case object CommandBlock extends TypeDescription
case class ReporterBlock(returnTypeDescription: TypeDescription) extends TypeDescription
case object ReferenceType extends TypeDescription
case object OptionalType extends TypeDescription
case class Repeatable(repeatedType: TypeDescription) extends TypeDescription
case object WildcardType extends TypeDescription
case class CustomType(name: String) extends TypeDescription

trait NamedType {
  def name: FormattedString
  def typeDescription: TypeDescription
}

sealed trait PrimitiveType

case class Reporter(returnType: TypeDescription) extends PrimitiveType
case object Command extends PrimitiveType

case class Primitive(
  fullName: String,
  primitiveType: PrimitiveType,
  description: FormattedString,
  arguments: Seq[Seq[NamedType]],
  agentContext: Seq[AgentType] = Seq(Turtle, Patch, Link, Observer))
