# aboutlibraries config

Custom library metadata overrides for the [aboutlibraries](https://github.com/mikepenz/AboutLibraries) plugin.

Place `<uniqueId>.json` override files here to correct or supplement auto-detected license metadata.
The plugin reads this directory at build time; it is also a declared task input, so adding or changing
files here correctly invalidates the Gradle configuration cache for `generateLibraryDefinitions*` tasks.

See: https://github.com/mikepenz/AboutLibraries?tab=readme-ov-file#define-libraries
