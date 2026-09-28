# Argentum
Argentum is a client performance mod for Ornithe 1.8.9, based on the [Celeritas](https://git.taumc.org/embeddedt/celeritas) rendering engine.

## Features
A non-exhaustive list of features that currently exist:

- Rewritten terrain meshing from Celeritas
- Entity rendering:
  - Instancing for players, mobs, animals
  - Instancing for attachments such as armor, arrows, etc
  - Nametag batching
- Block entity rendering:
  - Instancing for block entities
  - Baking for block entities (chests and ender chests, signs with and without text, maps, item frames); overrides instancing for the supported block entities
  - Lightmap caching
- Optimized item rendering:
  - Glint instancing & caching
  - Item atlas creation
- Font rendering:
  - Batching for lots of text
  - Cache text width, height, and geometry
- An optimized cloud renderer
- HUD rendering:
  - Join draw calls for many inventory elements
- Entity and particle occlusion culling
- Miscellaneous performance tweaks:
  - Decoupled frame presentation (presents frames at a given frequency instead of every frame, thus mitigating buffer swapping bottlenecks on stronger GPUs)
  - Greedy render thread (avoids Thread.yield() calls, can help on weaker GPUs at the cost of new blocks being briefly invisible)
- A Celeritas-based video settings menu

A companion mod also exists under the [`extras`](/extras) folder, providing extra rendering customization and eye candy.

The [`cera`](/cera) subproject reimplements MCPatcher/OptiFine resource pack extensions.
