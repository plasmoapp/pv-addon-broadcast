package su.plo.voice.broadcast.proxy;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.slib.api.permission.PermissionDefault;
import su.plo.slib.api.permission.PermissionManager;
import su.plo.slib.api.proxy.event.command.McProxyCommandExecuteEvent;
import su.plo.slib.api.proxy.event.command.McProxyCommandsRegisterEvent;
import su.plo.slib.api.proxy.player.McProxyPlayer;
import su.plo.slib.api.proxy.server.McProxyServerInfo;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.proxy.PlasmoVoiceProxy;
import su.plo.voice.api.proxy.event.config.VoiceProxyConfigReloadedEvent;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoicePlayerManager;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.broadcast.BuildConstants;
import su.plo.voice.broadcast.SourceResult;
import su.plo.voice.broadcast.proxy.command.ProxyBroadcastCommand;
import su.plo.voice.broadcast.proxy.filter.ProxyBroadcastFilter;
import su.plo.voice.broadcast.proxy.filter.ServerBroadcastFilter;
import su.plo.voice.broadcast.state.BroadcastState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Addon(id = "pv-addon-broadcast", scope = AddonLoaderScope.PROXY, version = BuildConstants.VERSION, authors = {"Apehum"})
public final class ProxyBroadcastAddon extends BroadcastAddon {

    @InjectPlasmoVoice
    @Getter
    private PlasmoVoiceProxy voiceProxy;

    public ProxyBroadcastAddon() {
        McProxyCommandsRegisterEvent.INSTANCE.registerListener((commandManager, minecraftProxy) -> {
            PermissionManager permissions = minecraftProxy.getPermissionManager();

            permissions.register("pv.addon.broadcast.*", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.proxy", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.server", PermissionDefault.OP);

            commandManager.register(
                    "vbroadcastproxy",
                    new ProxyBroadcastCommand(this),
                    "vbcp"
            );
        });

        McProxyCommandExecuteEvent.INSTANCE.registerListener((source, command) -> {
            if (!(source instanceof McProxyPlayer)) return;

            McProxyPlayer player = (McProxyPlayer) source;

            // reset proxy source
            if (command.startsWith("vbroadcast ") || command.startsWith("vbc ")) {
                removeBroadcastSource(player.getUuid());
                stateStore.remove(player.getUuid());
            }
        });
    }

    @Override
    public void onAddonInitialize() {
        loadConfig("proxy");
    }

    @EventSubscribe
    public void onConfigLoaded(@NotNull VoiceProxyConfigReloadedEvent event) {
        loadConfig("proxy");
    }

    @Override
    public Optional<McTextComponent> getCurrentBroadcastWideMessage(@NotNull VoicePlayer player) {
        Optional<ServerBroadcastSource> source = getBroadcastSource(player, false);
        if (!source.isPresent()) return Optional.empty();

        Optional<BroadcastState> state = stateStore.getByPlayerId(player.getInstance().getUuid());
        if (!state.isPresent()) return Optional.empty();

        switch (state.get().type()) {
            case "proxy": {
                return Optional.of(McTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_wide",
                        "proxy"
                ));
            }
            case "server": {
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
        VoiceProxyPlayer player = (VoiceProxyPlayer) voicePlayer;

        switch (type) {
            case "proxy": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.proxy")) {
                    return SourceResult.NO_PERMISSION;
                }

                if (arguments.size() > 0) {
                    return SourceResult.BAD_ARGUMENTS;
                }

                ServerBroadcastSource source = getBroadcastSource(player);
                source.clearFilters();
                source.addFilter(new ProxyBroadcastFilter(player));
                source.setSender(player);

                sourceByPlayerId.put(player.getInstance().getUuid(), source);
                stateStore.put(player.getInstance().getUuid(), new BroadcastState(type, arguments));
                broadcastWidePrinter.reset(player);

                return SourceResult.SUCCESS;
            }
            case "server": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.server")) {
                    return SourceResult.NO_PERMISSION;
                }

                if (arguments.size() == 0) {
                    return SourceResult.BAD_ARGUMENTS;
                }

                List<McProxyServerInfo> servers = voiceProxy.getMinecraftServer().getServers()
                        .stream()
                        .filter(server -> arguments.contains(server.getName()))
                        .collect(Collectors.toList());

                if (servers.isEmpty()) return SourceResult.BAD_ARGUMENTS;

                ServerBroadcastSource source = getBroadcastSource(player);
                source.clearFilters();
                source.addFilter(new ServerBroadcastFilter(player, servers));
                source.setSender(player);

                sourceByPlayerId.put(player.getInstance().getUuid(), source);
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
        return voiceProxy.getPlayerManager();
    }

    @Override
    protected String getDefaultSourceType() {
        return null;
    }
}
