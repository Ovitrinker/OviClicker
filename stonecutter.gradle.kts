plugins {
    id("dev.kikugie.stonecutter")
}

// Active in the IDE/development state
stonecutter active "26.2.x"

// Siehe https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    // Inserted in the source via /*$ mod_version*/
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    // Constant for the Fabric API version of each node
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}
