#####Version 1.12.2-2.2.1

#####Version 1.12.2-2.2.0 - BUILT
Main changes:
- Introduced ModWinder, an in-game mod installer
- Moved the NBT serialization facilities around
- Deprecated `ICustomLocation` in favor of `ItemRenderRegistrationHandler`
- Added an asset overriding API

Shaders:
- Added the `saturation` base shader
- Added some convenience methods for shader registration
- Improved shader compilation error handling

Annotation Magic:
- Added `@EnhancedBusSubscriber`, an alternative to `@EventBusSubscriber`
- Allowed `@AutoRegister` to substitute `@ObjectHolder` for when the latter is too rigid

Miscellaneous:
- Added package-level documentation
- Reworked the project's build script

#####Version 1.12.2-2.1.0 - BUILT
Add screen shaders
Fix automatic registration for older mappings
Add reflection helper methods for obfuscation and method handles

#####Version History End