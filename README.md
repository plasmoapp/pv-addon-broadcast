![](https://i.imgur.com/grB8hjw.png)

<div>
    <a href="https://modrinth.com/mod/plasmo-voice">Plasmo Voice</a>
    <span> | </span>
    <a href="https://modrinth.com/plugin/pv-addon-broadcast">Modrinth</a>
    <span> | </span>
    <a href="https://github.com/plasmoapp/pv-addon-broadcast/">GitHub</a>
    <span> | </span>
    <a href="https://discord.com/invite/uueEqzwCJJ">Discord</a>
     <span> | </span>
    <a href="https://www.patreon.com/plasmomc">Patreon</a>
</div>

# pv-addon-broadcast

Server-side [Plasmo Voice](https://modrinth.com/mod/plasmo-voice) add-on.

Broadcast your voice to all players in the radius, world, server, or proxy.

## Installation

You can install this add-on on:

- Paper, Fabric, or Forge
  - Can set the broadcasting scope to a radius, world, or the whole server. Using the `/vbroadcast` or `/vbc` command.
- Bungee or Velocity
  - Can set the broadcasting scope to a server, or all servers connected to a proxy. Using the `/vbroadcastproxy` or `/vbcp` command.
- Both
  - Can use both commands

## Usage

1. Open Plasmo Voice menu `V` (by default), go to the `Activation` tab and configure the `Broadcast` activation.
2. Use a command to set the broadcasting scope:
  - `/vbc range <radius>`
  - `/vbc world <world name>`
  - `/vbc server` or `/vbcp server <server name>`
  - `/vbcp proxy`
3. Use the activation to broadcast in the scope you've selected. 

## Permissions

All permissions are only available to OP by default.

### Permission to use broadcasting

`pv.activation.priority`

### Broadcasting scope permissions

`pv.addon.broadcast.range`

`pv.addon.broadcast.world`

`pv.addon.broadcast.server`

`pv.addon.broadcast.proxy`
