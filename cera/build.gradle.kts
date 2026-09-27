plugins {
    id("mc")
}

version = rootProject.version

loom {
    accessWidenerPath = file("src/main/resources/cera.classtweaker")
}
