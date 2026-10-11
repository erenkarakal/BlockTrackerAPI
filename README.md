# BlockTrackerAPI

A 1.21.11+ Paper API for checking if a block was naturally generated or placed by a player.

Every block gets a single bit:
- `0` = natural (world generated)
- `1` = not natural (player placed).

The data is stored per world in SQLite or PDC and only modified chunks are stored.

**Disclaimer**: This plugin was made for personal use on my own server.
So I am not looking to add suggested features unless it's interesting.

---

## Concepts

### Chunks and sections

A Minecraft **chunk** is a 16x16 column of blocks spanning the full world height.
To store data compactly it's split into **sections**: 16x16x16 cubes (4096 blocks each).
A typical overworld (`-64` to `320`) has 24 sections per chunk.

```
chunk (16 x worldHeight x 16)
|--- section (16 x 16 x 16 = 4096 blocks)
```

### One bit per block

Each section is a 4096-bit bitmap, one bit per block, indexed by its position inside the section.

- `0` = natural
- `1` = not natural (player placed)

A section where nothing was ever placed is simply not saved.
Only sections that actually contain a `1` are stored.

---

## Usage

```yaml
depend: [BlockTrackerAPI]     # or softdepend
```

```java
BlockTracker tracker = BlockTrackerAPI.get();
if (tracker == null) {
    return;
}

Block block = ...;
if (tracker.isNatural(block)) {
    // world generated
} else {
    // placed by a player
}

tracker.setNatural(block, false);

Location location = ...;
tracker.setNatural(location, true);
```

```java
boolean isNatural(World world, int x, int y, int z);
boolean isNatural(Block block);
boolean isNatural(Location location);

void setNatural(World world, int x, int y, int z, boolean natural);
void setNatural(Block block, boolean natural);
void setNatural(Location location, boolean natural);
```

Note that the API assumes the block/location is currently loaded.
Setting a block updates it in memory and persists it when its chunk unloads.

---

## Configuration

```yaml
# storage:
#   pdc    - store data on each chunk's PDC. Should be faster, but will cause larger world files
#   sqlite - store data in a per-world SQLite database in this plugin's folder
storage: pdc

# worlds to skip entirely.
untracked-worlds:
  - example_world
```

- Changes to the config require a server restart.
- Switching backends does NOT migrate existing data!
- Worlds listed in `untracked-worlds` are never tracked. `isNatural` always returns `true`, and `setNatural` does nothing.

---

## Cases

| Event | Effect on the position |
|---|---|
| Player places a block | set to `1` (not natural) |
| Player breaks a block | set to `0` (natural) |
| Explosion destroys a block | set to `0` |
| Piston pushes a block | the flag moves with the block |

So placing a block and then breaking it returns the position to natural.

---

## Memory footprint

Untouched chunks cost nothing. Memory use has two separate parts, and only the first scales with the whole world.

### 1. Modified Chunk Index

Each world keeps an in memory set of chunk coordinates that have stored data, just the keys, not the section data.
It's a `long` set of roughly **~16 B per modified chunk**.

- 100,000 modified chunks = **~1.5 MB**
- 1,000,000 modified chunks = **~16 MB**

This is the only structure that grows with the total number of modified chunks in the world.

### 2. Loaded Section Bitmaps

When a modified chunk loads, its section bitmaps are read from SQLite and kept in memory.
When the chunk unloads they are written back and freed. So this part scales with
how many modified chunks are loaded at once (players x view distance), not with the database size.

- **Each populated section:** `long[64]` = 512 B payload + ~16 B header = **~528 B**
- **Each modified chunk:** `ChunkData` + its sections array + map entry = **~200 B**

Example: 3 players at view-distance 10 load a few thousand chunks, only the modified subset among those holds bitmaps.
