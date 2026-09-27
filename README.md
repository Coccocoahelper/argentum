# Argentum
Argentum is a client performance mod for Ornithe 1.8.9, based on the [Celeritas](https://git.taumc.org/embeddedt/celeritas) rendering engine.

## Features
A non-exhaustive list of features that currently exist:

- Rewritten terrain meshing from Celeritas
- Entity rendering improvements, including instancing for players, mobs, and animals (and attachments, like armor)
- Nametag batching
- Instancing and baking for block entities
- An optimized font renderer (batching and caching)
- An optimized HUD renderer (item atlas creation and element batching)
- An optimized cloud renderer
- Entity and particle occlusion culling
- Optionally decoupled frame presentation
- A Celeritas-based video settings menu

A companion mod also exists under the [`extras`](/extras) folder, providing extra rendering customization and eye candy.

The [`cera`](/cera) subproject reimplements MCPatcher/OptiFine resource pack extensions.
