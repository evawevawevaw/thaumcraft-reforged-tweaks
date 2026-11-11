# Third-party notices

## Classic Thaumcraft NEI Plugin presentation

Textures and the idea of the classic recipe presentation: **Thaumcraft NEI Plugin 1.7.10-1.7a** by **DjGiannuzz** ([EmanueleGiannuzzi](https://github.com/EmanueleGiannuzzi/Thaumcraft-NEI-Plugin)). Reference methods: `ArcaneShapedRecipeHandler.drawBackground`, `CrucibleRecipeHandler.drawBackground`, `InfusionRecipeHandler.drawBackground` and `AspectRecipeHandler.renderComponent`. They describe the atlas regions, visual geometry and scales used for the native EMI presentation.

[Thaumcraft NEI Plugin: Patched](https://github.com/Thenemex/Thaumcraft-NEI-Plugin-Patched) by **Thenemex** fixes recipe lookup and inherits the original rendering. Its licence permits addons with credit and a link, and prohibits redistributing an edited version of that library without permission. No edited library, NEI classes or patch implementation is distributed here. The original plugin contains no standalone license file, so no license for its Java implementation or assets is assumed.

The EMI renderer is newly written for the current public EMI/NeoForge APIs. The texture `thaumcraft:textures/gui/gui_researchbook_overlay.png` is supplied by the required Thaumcraft 4 Reforged installation and is not copied into this jar.

## Dependencies

Thaumcraft 4 Reforged is a separate required dependency and is not bundled. This notice does not expand its permissions to unrelated third-party implementations. There is no runtime dependency on the legacy NEI plugins.
