# NetLogo Extension Documenter

## What is this tool?

sbt plugin for generating documentation.
Takes a `documentation.conf` file in the root of an sbt project and turns it into a delicious README.
Uses HOCON (although other markup formats may supported in the future) for data and
mustache for document rendering.

## Using

Get started by adding the following to your `project/plugins.sbt`

```scala
resolvers += Resolver.url(
  "NetLogo-JVM",
  url("http://dl.bintray.com/content/netlogo/NetLogo-JVM"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("org.nlogo" % "netlogo-extension-documentation" % "0.6")
```

Then add the following to your `build.sbt`

```scala
enablePlugins(org.nlogo.build.ExtensionDocumentationPlugin)
```

Finally, add a simple documentation.conf

```hocon
extensionName = "my-ext"
markdownTemplate = """
# Your NetLogo Extension

This extension contains NetLogo primitives

{{#include}}BUILDING.md{{/include}}

## Primitives

{{#allPrimitives}}
{{{.}}}
{{/allPrimitives}}
"""
primTemplate = """
### `{{name}}`

{{{description}}}
"""
primitives = [
  {
    name: a-command,
    type: command,
    arguments: [ { name: my-turtle, type: turtle } ],
    description: "does thing to turtle"
  },
  {
    name: a-reporter,
    type: reporter,
    returns: anything,
    arguments: [ { name: list-of-stuff, type: list } ],
    description: "returns a thing from the list"
  }
]
```

Then run the `extensionDocument` sbt command and you'll see a shiny new README!

## Data Format

These are the recognized top-level keys in documentation.conf.

* `markdownTemplate`: Template for the README.md. Required to use `extensionDocument`.

* `primTemplate`: Template for rendering primitives in the README.md. This produces a list of strings, accessible within the `markdownTemplate` as `allPrimitives`.

* `primitives`: Array of primitive objects. Required to use `extensionDocument`.

* `tableOfContents`: Optional object mapping tags to ToC sections. Looks like `{ "tag1": "Full Section Name 1", "tag2": "Full Section Name 2" }`

* `additionalVariables`: Optional object with keys will be available to `markdownTemplate`.

* `filesToIncludeInManual`: Optional array of strings. If present, tells the NetLogo manual which files to include when building. It is recommended that instructions on how to build the extension, as well as licensing information be ommitted from the NetLogo manual.

* `extensionName`: Optional string. If present all prims will have the string, along with a colon, prefixed to their name.

### Primitive Objects

Primitives have the following keys:

* `name`: Required. Primitive will be ommitted if `name` is not present.
* `type`: Strongly Recommended. Must be either `command` or `reporter`. Defaults to `command` if not present.
* `returns`: Strongly Recommended for reporters. Specifies the return type of the primitive. Defaults to wildcard / anything if not present.
* `arguments`: Optional. List of arguments to the primitive. If the primitive accepts no arguments, this key may be omitted.
* `alternateArguments`: Optional. List of arguments to the primitive. Use this if the primitive accepts a different set of arguments than under arguments.
  An argument is an object `type` key and an optional `name` key. the `type` key should be one of the types listed below.
* `description`: Strongly recommended. Description of the primitive, in Markdown.

### Types

The following types are recognized. All other types are assumed to be a custom type.

* `""` or `"anything"`: wildcard type / any type
* `"list"`
* `"string"`
* `"boolean"`
* `"number"`
* `"patchset"`
* `"turtleset"`
* `"linkset"`
* `"turtle"`
* `"patch"`
* `"symbol"`
* `"command block"`
* `"optional command block"`
* `"reporter block"`
* `"command"`: anonymous command or name of command
* `"reporter"`: anonymous reporter or name of reporter
* `"reference"`
* `"code block"`
* `"repeatable x"`: Repeatable of x
* all others: custom type with specified name

## Rendering schema

### Markdown Template

* `include` is a lambda that includes files from `extensionDocumentationIncludePath`. This defauls to the project root, but may be changed. Use like: `{{#include}}FILENAME.md{{/include}}`
* `contents` is a list of table of contents objects that look like the following:
```
  {
    fullCategoryName: "Full Name here",
    shortCategoryName: "tagName",
    prims: <List of prims as described in Primitives template below>
  }
```
* `allPrimitives` is a list of strings created by running each primitive through the primTemplate.
  Typically this is iterated through like `{{#allPrimitives}}{{{.}}}{{/allPrimitives}}`.
* any variables from `additionalConfig` will be available. For instance, if you had `additionalConfig: { netlogoUrl = "ccl.northwestern.edu/netlogo/" }`, you could use the following in the markdown template: `[The NetLogo Website]({{netlogoUrl}})`.

### Primitives Template

* `name`
* `description` (remember to use `{{{`, `}}}`, as this is in markdown)
* `primitive`: a Primitive object (see PrimitiveData.scala)
* `examples`: a list of objects encapsulating argument sets. There will always be one element in `examples`, even if the `arguments` key wasn't specified. Examples contains the following:
  * `primitive`: the Primitive object this example belongs to (see PrimitiveData.scala)
  * `args`: a list of argument values from above (`NamedType` in PrimitiveData.scala). Each argument has the following keys:
    * `name`: the name given to the argument
    * `typeName`: The TypeName of the argument. Use `{{typeName.name}}` to get the human-readable name of the type
