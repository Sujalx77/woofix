WOOFIX - Minecraft 1.21.1 / NeoForge 21.1  (call your dog, send it home, one menu key)

BUILD: upload everything to GitHub, open Actions, download the "Woofix-jar" artifact,
unzip, and put woofix-1.1.0.jar in .minecraft/mods (delete the old heelandhome jar).

USE
- G opens the Woofix menu (rebindable). Pick dog + home, then Call or Send home.
- Look at a block, type a name, press "Set home". Dog sits on top of that block.
- "Link dog to home" gives each dog its own house.
- Optional quick keys (unbound by default): Call Dog, Send Dog Home.
- Commands: /dog call, /dog home set <name>, /dog home go [name]
- A message shows when the dog arrives home. A whistle plays when you call.

SETTINGS: .minecraft/config/woofix-common.toml (restart the game after editing)
  allowTeleport = false     -> dogs always walk, never teleport
  disableNearCall = true    -> calling ignores dogs already closer than nearCallDistance (32)
                              (sending home still works)
  whistleSound, crossDimension, walkRange, cooldownTicks, walkTimeoutTicks, walkOnlyTimeoutTicks
