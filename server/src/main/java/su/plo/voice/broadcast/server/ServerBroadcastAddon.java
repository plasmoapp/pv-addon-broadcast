package su.plo.voice.broadcast.server;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.slib.api.permission.PermissionDefault;
import su.plo.slib.api.permission.PermissionManager;
import su.plo.slib.api.server.event.command.McServerCommandsRegisterEvent;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoicePlayerManager;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.broadcast.BuildConstants;
import su.plo.voice.broadcast.SourceResult;
import su.plo.voice.broadcast.server.command.ServerBroadcastCommand;
import su.plo.voice.broadcast.server.source.GlobalBroadcastFilter;
import su.plo.voice.broadcast.server.source.RangeBroadcastFilter;
import su.plo.voice.broadcast.server.source.WorldBroadcastFilter;
import su.plo.voice.broadcast.state.BroadcastState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Addon(id = "pv-addon-broadcast", scope = AddonLoaderScope.SERVER, version = BuildConstants.VERSION, authors = {"Apehum"})
public final class ServerBroadcastAddon extends BroadcastAddon {

    @InjectPlasmoVoice
    @Getter
    private PlasmoVoiceServer voiceServer;

    public ServerBroadcastAddon() {
        McServerCommandsRegisterEvent.INSTANCE.registerListener((commandManager, minecraftServer) -> {
            PermissionManager permissions = minecraftServer.getPermissionManager();

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
    public Optional<McTextComponent> getCurrentBroadcastWideMessage(@NotNull VoicePlayer player) {
        Optional<ServerBroadcastSource> source = getBroadcastSource(player, false);
        if (!source.isPresent()) return Optional.empty();

        Optional<BroadcastState> state = stateStore.getByPlayerId(player.getInstance().getUuid());
        if (!state.isPresent()) return Optional.empty();

        switch (state.get().type()) {
            case "range": {
                return Optional.of(McTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_specific",
                        "range " + state.get().arguments().get(0))
                );
            }
            case "server": {
                return Optional.of(McTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_wide",
                        "server"
                ));
            }
            case "world": {
                return Optional.of(McTextComponent.translatable(
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
    public SourceResult initializeBroadcastSource(
            @NotNull VoicePlayer voicePlayer,
            @NotNull String type,
            @NotNull List<String> arguments
    ) {
        VoiceServerPlayer player = (VoiceServerPlayer) voicePlayer;

        switch (type) {
            case "range": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.range")) {
                    return SourceResult.NO_PERMISSION;
                }

                if (arguments.size() == 0) {
                    return SourceResult.BAD_ARGUMENTS;
                }

                int range;
                try {
                    range = Integer.parseInt(arguments.get(0));
                } catch (NumberFormatException ignored) {
                    return SourceResult.BAD_ARGUMENTS;
                }

                if (range <= 0) return SourceResult.BAD_ARGUMENTS;

                ServerBroadcastSource broadcastSource = getBroadcastSource(player, source -> {
                    source.clearFilters();
                    source.addFilter(new RangeBroadcastFilter(player, range));
                    source.setSender(player);
                });

                sourceByPlayerId.put(player.getInstance().getUuid(), broadcastSource);
                stateStore.put(player.getInstance().getUuid(), new BroadcastState(type, arguments));
                broadcastWidePrinter.reset(player);

                return SourceResult.SUCCESS;
            }
            case "server": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.server")) {
                    return SourceResult.NO_PERMISSION;
                }

                ServerBroadcastSource broadcastSource = getBroadcastSource(player, source -> {
                    source.clearFilters();
                    source.addFilter(new GlobalBroadcastFilter(player));
                    source.setSender(player);
                });

                sourceByPlayerId.put(player.getInstance().getUuid(), broadcastSource);
                stateStore.put(player.getInstance().getUuid(), new BroadcastState(type, arguments));
                broadcastWidePrinter.reset(player);

                return SourceResult.SUCCESS;
            }
            case "world": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.world")) {
                    return SourceResult.NO_PERMISSION;
                }

                if (arguments.size() == 0) {
                    return SourceResult.BAD_ARGUMENTS;
                }

                List<String> argumentsList = ImmutableList.copyOf(arguments);
                List<McServerWorld> worlds = voiceServer.getMinecraftServer().getWorlds().stream()
                        .filter(world -> argumentsList.contains(world.getName()))
                        .collect(Collectors.toList());

                if (worlds.isEmpty()) {
                    return SourceResult.BAD_ARGUMENTS;
                }

                ServerBroadcastSource broadcastSource = getBroadcastSource(player, source -> {
                    source.clearFilters();
                    source.addFilter(new WorldBroadcastFilter(player, worlds));
                    source.setSender(player);
                });

                sourceByPlayerId.put(player.getInstance().getUuid(), broadcastSource);
                stateStore.put(player.getInstance().getUuid(), new BroadcastState(type, arguments));
                broadcastWidePrinter.reset(player);

                return SourceResult.SUCCESS;
            }
            default: {
                return SourceResult.UNKNOWN;
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
