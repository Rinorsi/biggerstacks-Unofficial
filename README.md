# Bigger Stacks (Unofficial)

NeoForge 1.21.1 fork that raises the stack size for almost every item. Global caps live in `biggerstacks-rules.xml`, and one-off overrides (totems, potions, enchanted books, etc.) are described in `biggerstacks-template-overrides.json`.

## How to use

1. Place the jar in both client and server `mods/` folders.
2. Run `/biggerstacks quicksetup` in-game to tweak the three preset categories, or edit `run/config/biggerstacks-rules.xml` manually if you prefer plain XML.
3. To stack normally unstackable items (Totem of Undying, custom items, tags), add entries inside `biggerstacks-template-overrides.json`.