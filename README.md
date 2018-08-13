# LadyLib
Ladysnake's library mod for minecraft modding

## Goals
This library aims to alleviate some pains of modding as well as to provide utilities for various things.

## Installation
This project can be used in two ways.
The first is through the use of git submodules, though this is intended mainly for contributors. 
You can see an example build.gradle for this in the [LadyLibProject](https://github.com/Ladysnake/LadyLibProject).
The other is as a proper gradle dependency through [jitpack](https://jitpack.io/#Pyrofab/Ladylib/).

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