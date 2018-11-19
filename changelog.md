##### Version 1.12.2-2.4.0 - BUILT
Shaders:
- Overhauled the screen/post-process shader system
- Deprecated old screen shaders methods
- Made the /ladylib_shader_reload command reload shader resources using Forge's new selective resource reloading
- Dynamic lights now use a post process shader

Reflection:
- Added generic typed method handles
- Added a new reflection helper that uses said typed method handles

Kotlin:
- Added a Kotlin module containing a few methods smoothing the experience for Forgelin fans

Bugfixes:
- Fixed the """Enhanced""" Event Subscriber not registering any class after an invalid one

##### Version 1.12.2-2.3.2 - BUILT
Fixed automatic capability registration behaving opposite to what the javadoc says

##### Version 1.12.2-2.3.1 - BUILT
Miscellaneous:
- Added `@CalledThroughReflection` and `@PublicApi` annotations for documenting indirectly called methods
- Added the ability to specify the owning mod on EnhancedBusSubscriber
- Updated installation instructions
- Removed the ability to download any mod not made by Ladysnake to better comply with Twitch TOS (sorry lads)

Bugfixes:
- Fixed automatic capability registrations when defaults are used

##### Version 1.12.2-2.3.0 - BUILT
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