package su.plo.voice.broadcast.server.command;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.api.server.command.MinecraftCommand;
import su.plo.lib.api.server.command.MinecraftCommandSource;
import su.plo.lib.api.server.player.MinecraftServerPlayer;
import su.plo.lib.api.server.world.MinecraftServerWorld;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.server.ServerBroadcastAddon;
import su.plo.voice.broadcast.source.BroadcastSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ServerBroadcastCommand implements MinecraftCommand {

    private static final List<String> SUB_COMMANDS = ImmutableList.of("range", "server", "world");

    private final ServerBroadcastAddon addon;

    @Override
    public void execute(@NotNull MinecraftCommandSource source, @NotNull String[] arguments) {
        if (!(source instanceof MinecraftServerPlayer)) {
            source.sendMessage(MinecraftTextComponent.translatable("pv.error.player_only_command"));
            return;
        }

        if (arguments.length == 0) {
            source.sendMessage(MinecraftTextComponent.translatable("pv.addon.broadcast.command.usage"));
            return;
        }

        MinecraftServerPlayer serverPlayer = (MinecraftServerPlayer) source;
        VoiceServerPlayer player = addon.getVoiceServer().getPlayerManager().getPlayerById(serverPlayer.getUUID())
                .orElseThrow(() -> new IllegalStateException("Player not found"));

        String type = arguments[0];
        List<String> argumentsList = ImmutableList.copyOf(arguments);
        argumentsList = argumentsList.size() > 1
                ? argumentsList.subList(1, argumentsList.size())
                : Collections.emptyList();

        BroadcastSource.Result result = addon.initializeBroadcastSource(player, type, argumentsList);
        switch (result) {
            case NO_PERMISSION:
                source.sendMessage(MinecraftTextComponent.translatable("pv.error.no_permissions"));
                break;
            case UNKNOWN:
                source.sendMessage(MinecraftTextComponent.translatable("pv.addon.broadcast.command.usage"));
                break;
            default:
                serverPlayer.sendMessage(getMessage(type, argumentsList, result));
                break;
        }
    }

    @Override
    public List<String> suggest(@NotNull MinecraftCommandSource source, @NotNull String[] arguments) {
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

        if (subCommand.equals("world") && hasPermission(source, "world")) {
            List<String> argumentsList = ImmutableList.copyOf(arguments);

            return addon.getVoiceServer()
                    .getMinecraftServer()
                    .getWorlds()
                    .stream()
                    .map(MinecraftServerWorld::getKey)
                    .filter((key) -> key.startsWith(arguments[arguments.length - 1]) && !argumentsList.contains(key))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(@NotNull MinecraftCommandSource source, @Nullable String[] arguments) {
        return source.hasPermission("pv.addon.broadcast.*") ||
                SUB_COMMANDS.stream().anyMatch((command) -> source.hasPermission("pv.addon.broadcast." + command));
    }

    private boolean hasPermission(@NotNull MinecraftCommandSource source, @NotNull String command) {
        return source.hasPermission("pv.addon.broadcast.*") ||
                source.hasPermission("pv.addon.broadcast." + command);
    }

    private MinecraftTextComponent getMessage(@NotNull String type,
                                              @NotNull List<String> arguments,
                                              @NotNull BroadcastSource.Result result) {
        switch (type) {
            case "range":
                return result == BroadcastSource.Result.SUCCESS
                        ? MinecraftTextComponent.translatable("pv.addon.broadcast.command.range_set")
                        : MinecraftTextComponent.translatable("pv.addon.broadcast.command.range_usage");

            case "server": {
                return result == BroadcastSource.Result.SUCCESS
                        ? MinecraftTextComponent.translatable("pv.addon.broadcast.command.server_set")
                        : MinecraftTextComponent.translatable("pv.addon.broadcast.command.server_usage");
            }
            case "world": {
                return result == BroadcastSource.Result.SUCCESS
                        ? MinecraftTextComponent.translatable("pv.addon.broadcast.command.world_set", String.join(", ", arguments))
                        : MinecraftTextComponent.translatable("pv.addon.broadcast.command.world_usage");
            }
            default:
                return MinecraftTextComponent.translatable("pv.addon.broadcast.command.usage");
        }
    }
}
