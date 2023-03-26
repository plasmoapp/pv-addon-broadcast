package su.plo.voice.broadcast.server;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.api.server.event.command.ServerCommandsRegisterEvent;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.lib.api.server.permission.PermissionsManager;
import su.plo.lib.api.server.world.MinecraftServerWorld;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent;
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

@Addon(id = "broadcast", scope = AddonLoaderScope.SERVER, version = "1.0.0", authors = {"Apehum"})
public final class ServerBroadcastAddon extends BroadcastAddon {

    @Inject
    @Getter
    private PlasmoVoiceServer voiceServer;

    public ServerBroadcastAddon() {
        ServerCommandsRegisterEvent.INSTANCE.registerListener((commandManager, minecraftServer) -> {
            PermissionsManager permissions = minecraftServer.getPermissionsManager();

            permissions.register("pv.addon.broadcast.*", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.range", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.server", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.world", PermissionDefault.OP);

            commandManager.register(
                    "vbroadcast",
                    new ServerBroadcastCommand(this),
                    "vbc"
            );
        });
    }

    @Override
    public void onAddonInitialize() {
        loadConfig("server");
    }

    @EventSubscribe
    public void onConfigLoaded(@NotNull VoiceServerConfigReloadedEvent event) {
        loadConfig("server");
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
                        new RangeBroadcastSource(directSource, player, range)
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
                        new GlobalBroadcastSource(directSource, player)
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
                        new WorldBroadcastSource(directSource, player, worlds)
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
    protected String getDefaultSourceType() {
        return "server";
    }
}
