# LadyLib
Ladysnake's library mod for minecraft modding

## Goals
This library aims to alleviate some pains of modding as well as to provide utilities for various things.

## Installation
To use this library in your workspace, the main way is to declare it as a gradle dependency with [jitpack](https://jitpack.io/#Pyrofab/Ladylib/). You are encouraged to use Forge's [contained dependencies feature](https://mcforge.readthedocs.io/en/latest/gettingstarted/dependencymanagement/#dependency-extraction) to ship the library in your mod's jar.

Sample code to add to your buildscript:
```gradle
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

configurations {
    contained
    contained.transitive = false
}

dependencies {
    deobfCompile 'com.github.Ladysnake:Ladylib:2.3.0' // replace with latest ladylib version (even better, put it in gradle.properties)
    contained 'com.github.Ladysnake:Ladylib:2.3.0'
}

jar {
    from(configurations.contained.files) {
        include '*'
        into 'META-INF/libraries'
    }
    manifest {
        attributes([
                'ContainedDeps': configurations.contained.files.collect { it.name }.join(" "),
                'Maven-Artifact': "${project.group}:${project.archivesBaseName}:${project.version}",
                'Timestamp'     : System.currentTimeMillis()
        ])
    }
}
```

## Main features
* Annotation-based registration
* Helper methods for registration (because the two aren't mutually exclusive)
* Generation of stub model files
* NBT Serialization based on [Gson](https://github.com/google/gson)
* Built-in methods to interact with online JSON APIs
* Simplified capabilities
* Extremely customizable particles
* A framework for shaders usage
* Programmatic overriding of vanilla resources
* Actual documentation (yes, I count that as a feature)

## ModWinder
ModWinder is an integrated installer for [Ladysnake approved mods](https://ladysnake.glitch.me/milksnake-bar). 
It works as a child mod of LadyLib, and allows players to easily install and update mods from inside the game.

## [Milksnake](Milksnake/README.md)
Milksnake is a sub-project [distributed on curseforge](https://minecraft.curseforge.com/projects/milksnake) containing
LadyLib and adding a few cosmetic features.
