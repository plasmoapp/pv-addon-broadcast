package su.plo.voice.broadcast.server;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.lib.api.server.permission.PermissionsManager;
import su.plo.lib.api.server.world.MinecraftServerWorld;
import su.plo.voice.api.addon.AddonScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.line.ServerSourceLineManager;
import su.plo.voice.api.server.audio.source.BaseServerSourceManager;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.event.VoiceServerInitializeEvent;
import su.plo.voice.api.server.event.command.CommandsRegisterEvent;
import su.plo.voice.api.server.event.config.VoiceServerConfigLoadedEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoicePlayerManager;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.broadcast.server.command.ServerBroadcastCommand;
import su.plo.voice.broadcast.server.source.GlobalBroadcastSource;
import su.plo.voice.broadcast.server.source.RangeBroadcastSource;
import su.plo.voice.broadcast.server.source.WorldBroadcastSource;
import su.plo.voice.broadcast.source.BroadcastSource;
import su.plo.voice.broadcast.state.BroadcastState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Addon(id = "broadcast", scope = AddonScope.SERVER, version = "1.0.0", authors = {"Apehum"})
public final class ServerBroadcastAddon extends BroadcastAddon {

    private PlasmoVoiceServer voiceServer;

    @EventSubscribe
    public void onServerInitialize(@NotNull VoiceServerInitializeEvent event) {
        this.voiceServer = event.getServer();
    }

    @EventSubscribe
    public void onConfigLoaded(@NotNull VoiceServerConfigLoadedEvent event) {
        PlasmoVoiceServer voiceServer = event.getServer();

        loadConfig(
                voiceServer,
                voiceServer.getLanguages(),
                voiceServer.getActivationManager(),
                voiceServer.getSourceLineManager(),
                voiceServer.getUdpConnectionManager(),
                voiceServer.getMinecraftServer().getPermissionsManager(),
                "server"
        );
    }

    @EventSubscribe
    public void onCommandsRegister(@NotNull CommandsRegisterEvent event) {
        PermissionsManager permissions = event.getVoiceServer().getMinecraftServer().getPermissionsManager();

        permissions.register("pv.addon.broadcast.*", PermissionDefault.OP);
        permissions.register("pv.addon.broadcast.range", PermissionDefault.OP);
        permissions.register("pv.addon.broadcast.server", PermissionDefault.OP);
        permissions.register("pv.addon.broadcast.world", PermissionDefault.OP);

        event.getCommandManager().register(
                "vbroadcast",
                new ServerBroadcastCommand(event.getVoiceServer(), this),
                "vbc"
        );
    }

    @Override
    public Optional<MinecraftTextComponent> getCurrentBroadcastWideMessage(@NotNull VoicePlayer player) {
        Optional<BroadcastSource<?>> source = getBroadcastSource(player, false);
        if (!source.isPresent()) return Optional.empty();

        Optional<BroadcastState> state = stateStore.getByPlayerId(player.getInstance().getUUID());
        if (!state.isPresent()) return Optional.empty();

        switch (state.get().type()) {
            case "range": {
                return Optional.of(MinecraftTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_specific",
                        "range " + state.get().arguments().get(0))
                );
            }
            case "server": {
                return Optional.of(MinecraftTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_wide",
                        "server"
                ));
            }
            case "world": {
                return Optional.of(MinecraftTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_specific",
                        String.join(", ", state.get().arguments())
                ));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public BroadcastSource.Result initializeBroadcastSource(@NotNull VoicePlayer voicePlayer,
                                                            @NotNull String type,
                                                            @NotNull List<String> arguments) {
        VoiceServerPlayer player = (VoiceServerPlayer) voicePlayer;

        switch (type) {
            case "range": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.range")) {
                    return BroadcastSource.Result.NO_PERMISSION;
                }

                if (arguments.size() == 0) {
                    return BroadcastSource.Result.BAD_ARGUMENTS;
                }

                int range;
                try {
                    range = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException ignored) {
                    return BroadcastSource.Result.BAD_ARGUMENTS;
                }

                if (range <= 0) return BroadcastSource.Result.BAD_ARGUMENTS;

                ServerDirectSource directSource = getDirectSource(player);
                sourceByPlayerId.put(
                        player.getInstance().getUUID(),
                        new RangeBroadcastSource(voiceServer.getSourceManager(), directSource, player, range)
                );
                stateStore.put(player.getInstance().getUUID(), new BroadcastState(type, arguments));

                return BroadcastSource.Result.SUCCESS;
            }
            case "server": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.server")) {
                    return BroadcastSource.Result.NO_PERMISSION;
                }

                ServerDirectSource directSource = getDirectSource(player);
                sourceByPlayerId.put(
                        player.getInstance().getUUID(),
                        new GlobalBroadcastSource(voiceServer.getSourceManager(), directSource, player)
                );
                stateStore.put(player.getInstance().getUUID(), new BroadcastState(type, arguments));

                return BroadcastSource.Result.SUCCESS;
            }
            case "world": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.world")) {
                    return BroadcastSource.Result.NO_PERMISSION;
                }

                if (arguments.size() == 0) {
                    return BroadcastSource.Result.BAD_ARGUMENTS;
                }

                List<String> argumentsList = ImmutableList.copyOf(arguments);
                List<MinecraftServerWorld> worlds = voiceServer.getMinecraftServer().getWorlds().stream()
                        .filter(world -> argumentsList.contains(world.getKey()))
                        .collect(Collectors.toList());

                if (worlds.isEmpty()) {
                    return BroadcastSource.Result.BAD_ARGUMENTS;
                }

                ServerDirectSource directSource = getDirectSource(player);
                sourceByPlayerId.put(
                        player.getInstance().getUUID(),
                        new WorldBroadcastSource(voiceServer.getSourceManager(), directSource, player, worlds)
                );
                stateStore.put(player.getInstance().getUUID(), new BroadcastState(type, arguments));

                return BroadcastSource.Result.SUCCESS;
            }
            default: {
                return BroadcastSource.Result.UNKNOWN;
            }
        }
    }

    @Override
    public VoicePlayerManager<?> getPlayerManager() {
        return voiceServer.getPlayerManager();
    }

    @Override
    public BaseServerSourceManager getSourceManager() {
        return voiceServer.getSourceManager();
    }

    @Override
    public ServerSourceLineManager getSourceLineManager() {
        return voiceServer.getSourceLineManager();
    }
}
