rootProject.name = "nametagger"

include("api", "platform-util")
include("spigot")

include("example", "example:spigot")

setChildrenProjectNames(rootProject.name, rootProject)

fun setChildrenProjectNames(prefix: String, parent: ProjectDescriptor) {
  for (it in parent.children)
  {
    it.name = "${prefix}-${it.name}"
    setChildrenProjectNames(prefix, it)
  }
}