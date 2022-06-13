tasks.withType(Jar::class) {
    manifest {
        val modId = project.parent?.property("mod_id");
        attributes(
            "Automatic-Module-Name" to "{$modId}"
        )
    }
}
