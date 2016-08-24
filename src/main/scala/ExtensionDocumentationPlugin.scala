package org.nlogo.build

import sbt._, Keys._

import java.nio.file.Files

object ExtensionDocumentationPlugin extends AutoPlugin {
  object autoImport {
    val extensionDocumentationConfigFile = settingKey[File]("documentationConfigFile")
    val extensionDocument = taskKey[File]("create readme in markdown (for github)")
  }

  lazy val extensionDocumentationIncludePath = settingKey[File]("path from which to locate includes when generating documentation")
  lazy val extensionDocumentationReadmeTarget = settingKey[File]("path to which generated documentation is written")

  import autoImport._

  override lazy val projectSettings = Seq(
    extensionDocumentationConfigFile := baseDirectory.value / "documentation.conf",
    extensionDocumentationIncludePath := baseDirectory.value,
    extensionDocumentationReadmeTarget := baseDirectory.value / "README.md",
    extensionDocument := {
      val configDocument = HoconParser.parseConfigFile(extensionDocumentationConfigFile.value)
      val documentationConfig = HoconParser.parseConfig(configDocument)
      val parsedPrims = HoconParser.parsePrimitives(configDocument)
      parsedPrims.warnings.foreach { w => streams.value.log.warn(w.message) }

      val documentationText = Documenter.documentAll(documentationConfig, parsedPrims.primitives, extensionDocumentationIncludePath.value.toPath)
      Files.write(extensionDocumentationReadmeTarget.value.toPath, documentationText.getBytes)
      extensionDocumentationReadmeTarget.value
    }
  )
}
