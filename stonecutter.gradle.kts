plugins {
    id("dev.kikugie.stonecutter")
}

// Aktiv im IDE-/Entwicklungszustand
stonecutter active "26.2.x"

// Siehe https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    // Wird im Quellcode ueber /*$ mod_version*/ eingesetzt
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    // Konstante fuer die Fabric-API-Version des jeweiligen Knotens
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}
