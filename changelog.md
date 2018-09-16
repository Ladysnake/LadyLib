##### Version 1.12.2-2.3 - BUILT

Main changes:
- Implemented cheap dynamic lights as an alternative to Albedo

Shaders:
- Added `/ladylib_shader_reload` to quickly reload shaders without relying on F3-T
- Optimized shader uniform lookup through a cache
- Shaders now get freed when they are replaced
- Added generic methods to set uniform and attributes values
- Actually implemented setting uniforms on screen shaders

Bugfixes:
- Fixed installation state localization not being updated with lang setting

##### Version 1.12.2-2.2.2 - BUILT
ModWinder changes:
- Allowed multiple mod lists to coexist
- Added an "update all" button
- Added german translation by Lukas
- Updated french translation
- Fixed mod deletion
- Now allows mods without Maven-Artifact to be installed
- The changelog button is now disabled when no changelog is available

Miscellaneous:
- Removed the limit on queued json requests

##### Version 1.12.2-2.2.1 - BUILT

- Made ResourceProxy slightly more versatile
- Various ModWinder fixes

##### Version 1.12.2-2.2.0 - BUILT

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

##### Version 1.12.2-2.1.0 - BUILT

Add screen shaders
Fix automatic registration for older mappings
Add reflection helper methods for obfuscation and method handles

##### Version History End