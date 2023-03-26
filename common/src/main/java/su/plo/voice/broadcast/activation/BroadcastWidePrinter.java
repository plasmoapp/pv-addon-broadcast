package su.plo.voice.broadcast.activation;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.broadcast.config.BroadcastConfig;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class BroadcastWidePrinter {

    private final BroadcastAddon addon;

    private final Map<UUID, Long> lastPrint = Maps.newConcurrentMap();

    public void sendMessage(@NotNull VoicePlayer player) {
        if (addon.getConfig().showCurrentBroadcastWide() == BroadcastConfig.ShowCurrentBroadcastWide.HIDDEN) return;

        addon.getCurrentBroadcastWideMessage(player).ifPresent((message) -> {
            if (addon.getConfig().showCurrentBroadcastWide() == BroadcastConfig.ShowCurrentBroadcastWide.ACTION_BAR) {
                sendActionBar(player, message);
            } else {
                sendChat(player, message);
            }
        });
    }

    public void reset(@NotNull VoicePlayer player) {
        lastPrint.remove(player.getInstance().getUUID());
    }

    @EventSubscribe
    public void onPlayerQuit(@NotNull UdpClientDisconnectedEvent event) {
        lastPrint.remove(event.getConnection().getPlayer().getInstance().getUUID());
    }

    private synchronized void sendChat(@NotNull VoicePlayer player, @NotNull MinecraftTextComponent message) {
        if (lastPrint.containsKey(player.getInstance().getUUID())) return;

        player.getInstance().sendMessage(message);
        lastPrint.put(player.getInstance().getUUID(), System.currentTimeMillis());
    }

    private synchronized void sendActionBar(@NotNull VoicePlayer player, @NotNull MinecraftTextComponent message) {
        long now = System.currentTimeMillis();
        long last = lastPrint.getOrDefault(player.getInstance().getUUID(), 0L);
        if (now - last > 250L) {
            lastPrint.put(player.getInstance().getUUID(), now);

            player.getInstance().sendActionBar(message);
        }
    }
}
