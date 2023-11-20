package su.plo.voice.broadcast.proxy.command;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.slib.api.command.McCommandSource;
import su.plo.slib.api.proxy.command.McProxyCommand;
import su.plo.slib.api.proxy.player.McProxyPlayer;
import su.plo.slib.api.proxy.server.McProxyServerInfo;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.broadcast.SourceResult;
import su.plo.voice.broadcast.proxy.ProxyBroadcastAddon;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class ProxyBroadcastCommand implements McProxyCommand {

    private static final List<String> SUB_COMMANDS = ImmutableList.of("server", "proxy");

    private final ProxyBroadcastAddon addon;

    @Override
    public void execute(@NotNull McCommandSource source, @NotNull String[] arguments) {
        if (!(source instanceof McProxyPlayer)) {
            source.sendMessage(McTextComponent.translatable("pv.error.player_only_command"));
            return;
        }

        if (arguments.length == 0) {
            source.sendMessage(McTextComponent.translatable("pv.addon.broadcast.command.usage"));
            return;
        }

        McProxyPlayer serverPlayer = (McProxyPlayer) source;
        VoiceProxyPlayer player = addon.getVoiceProxy().getPlayerManager().getPlayerById(serverPlayer.getUuid())
                .orElseThrow(() -> new IllegalStateException("Player not found"));

        String type = arguments[0];
        List<String> argumentsList = ImmutableList.copyOf(arguments);
        argumentsList = argumentsList.size() > 1
                ? argumentsList.subList(1, argumentsList.size())
                : Collections.emptyList();

        SourceResult result = addon.initializeBroadcastSource(player, type, argumentsList);
        switch (result) {
            case NO_PERMISSION:
                source.sendMessage(McTextComponent.translatable("pv.error.no_permissions"));
                break;
            case UNKNOWN:
                source.sendMessage(McTextComponent.translatable("pv.addon.broadcast.command.usage"));
                break;
            default:
                serverPlayer.sendMessage(getMessage(type, argumentsList, result));
                break;
        }
    }

    @Override
    public List<String> suggest(@NotNull McCommandSource source, @NotNull String[] arguments) {
        if (arguments.length == 0)
            return SUB_COMMANDS.stream()
                    .filter((command) -> hasPermission(source, command))
                    .collect(Collectors.toList());

        String subCommand = arguments[0];
        if (arguments.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter((command) -> command.startsWith(subCommand))
                    .filter((command) -> hasPermission(source, command))
                    .collect(Collectors.toList());
        }

        if (subCommand.equals("server") && hasPermission(source, "server")) {
            List<String> argumentsList = ImmutableList.copyOf(arguments);

            return addon.getVoiceProxy()
                    .getMinecraftServer()
                    .getServers()
                    .stream()
                    .map(McProxyServerInfo::getName)
                    .filter((key) -> key.startsWith(arguments[arguments.length - 1]) && !argumentsList.contains(key))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(@NotNull McCommandSource source, @Nullable String[] arguments) {
        return source.hasPermission("pv.addon.broadcast.*") ||
                SUB_COMMANDS.stream().anyMatch((command) -> source.hasPermission("pv.addon.broadcast." + command));
    }

    private boolean hasPermission(@NotNull McCommandSource source, @NotNull String subCommand) {
        return source.hasPermission("pv.addon.broadcast.*") ||
                source.hasPermission("pv.addon.broadcast." + subCommand);
    }

    private McTextComponent getMessage(
            @NotNull String type,
            @NotNull List<String> arguments,
            @NotNull SourceResult result
    ) {
        switch (type) {
            case "server": {
                return result == SourceResult.SUCCESS
                        ? McTextComponent.translatable("pv.addon.broadcast.command.server_set", String.join(", ", arguments))
                        : McTextComponent.translatable("pv.addon.broadcast.command.server_usage");
            }
            case "proxy": {
                return result == SourceResult.SUCCESS
                        ? McTextComponent.translatable("pv.addon.broadcast.command.proxy_set")
                        : McTextComponent.translatable("pv.addon.broadcast.command.usage");
            }
            default:
                return McTextComponent.translatable("pv.addon.broadcast.command.usage");
        }
    }
}
