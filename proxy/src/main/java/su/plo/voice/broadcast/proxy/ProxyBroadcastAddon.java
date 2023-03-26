package su.plo.voice.broadcast.proxy;

import com.google.inject.Inject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.api.proxy.event.command.MinecraftCommandExecuteEvent;
import su.plo.lib.api.proxy.player.MinecraftProxyPlayer;
import su.plo.lib.api.proxy.server.MinecraftProxyServerInfo;
import su.plo.lib.api.server.event.command.ProxyCommandsRegisterEvent;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.lib.api.server.permission.PermissionsManager;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.proxy.PlasmoVoiceProxy;
import su.plo.voice.api.proxy.event.config.VoiceProxyConfigReloadedEvent;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoicePlayerManager;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.broadcast.proxy.command.ProxyBroadcastCommand;
import su.plo.voice.broadcast.proxy.source.GlobalBroadcastSource;
import su.plo.voice.broadcast.proxy.source.ServerBroadcastSource;
import su.plo.voice.broadcast.source.BroadcastSource;
import su.plo.voice.broadcast.state.BroadcastState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Addon(id = "pv-addon-broadcast", scope = AddonLoaderScope.PROXY, version = "1.0.0", authors = {"Apehum"})
public final class ProxyBroadcastAddon extends BroadcastAddon {

    @Inject
    @Getter
    private PlasmoVoiceProxy voiceProxy;

    public ProxyBroadcastAddon() {
        ProxyCommandsRegisterEvent.INSTANCE.registerListener((commandManager, minecraftProxy) -> {
            PermissionsManager permissions = minecraftProxy.getPermissionsManager();

            permissions.register("pv.addon.broadcast.*", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.proxy", PermissionDefault.OP);
            permissions.register("pv.addon.broadcast.server", PermissionDefault.OP);

            commandManager.register(
                    "vbroadcastproxy",
                    new ProxyBroadcastCommand(this),
                    "vbcp"
            );
        });
    }

    @EventSubscribe
    public void onCommandExecute(@NotNull MinecraftCommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof MinecraftProxyPlayer)) return;

        MinecraftProxyPlayer player = (MinecraftProxyPlayer) event.getCommandSource();

        // reset proxy source
        if (event.getCommand().startsWith("vbroadcast ") || event.getCommand().startsWith("vbc ")) {
            removeBroadcastSource(player.getUUID());
            stateStore.remove(player.getUUID());
        }
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
    public Optional<MinecraftTextComponent> getCurrentBroadcastWideMessage(@NotNull VoicePlayer player) {
        Optional<BroadcastSource<?>> source = getBroadcastSource(player, false);
        if (!source.isPresent()) return Optional.empty();

        Optional<BroadcastState> state = stateStore.getByPlayerId(player.getInstance().getUUID());
        if (!state.isPresent()) return Optional.empty();

        switch (state.get().type()) {
            case "proxy": {
                return Optional.of(MinecraftTextComponent.translatable(
                        "pv.addon.broadcast.broadcasting_wide",
                        "proxy"
                ));
            }
            case "server": {
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
        VoiceProxyPlayer player = (VoiceProxyPlayer) voicePlayer;

        switch (type) {
            case "proxy": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.proxy")) {
                    return BroadcastSource.Result.NO_PERMISSION;
                }

                if (arguments.size() > 0) {
                    return BroadcastSource.Result.BAD_ARGUMENTS;
                }

                ServerDirectSource directSource = getDirectSource(player);
                sourceByPlayerId.put(
                        player.getInstance().getUUID(),
                        new GlobalBroadcastSource(directSource, player)
                );
                stateStore.put(player.getInstance().getUUID(), new BroadcastState(type, arguments));

                return BroadcastSource.Result.SUCCESS;
            }
            case "server": {
                if (!player.getInstance().hasPermission("pv.addon.broadcast.server")) {
                    return BroadcastSource.Result.NO_PERMISSION;
                }

                if (arguments.size() == 0) {
                    return BroadcastSource.Result.BAD_ARGUMENTS;
                }

                List<MinecraftProxyServerInfo> servers = voiceProxy.getMinecraftServer().getServers()
                        .stream()
                        .filter(server -> arguments.contains(server.getName()))
                        .collect(Collectors.toList());

                if (servers.isEmpty()) return BroadcastSource.Result.BAD_ARGUMENTS;

                ServerDirectSource directSource = getDirectSource(player);
                sourceByPlayerId.put(
                        player.getInstance().getUUID(),
                        new ServerBroadcastSource(directSource, player, servers)
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
        return voiceProxy.getPlayerManager();
    }

    @Override
    protected String getDefaultSourceType() {
        return "proxy";
    }
}
