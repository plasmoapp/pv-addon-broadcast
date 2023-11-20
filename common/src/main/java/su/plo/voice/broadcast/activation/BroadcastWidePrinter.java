package su.plo.voice.broadcast.activation;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.chat.component.McTextComponent;
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
        lastPrint.remove(player.getInstance().getUuid());
    }

    @EventSubscribe
    public void onPlayerQuit(@NotNull UdpClientDisconnectedEvent event) {
        lastPrint.remove(event.getConnection().getPlayer().getInstance().getUuid());
    }

    private synchronized void sendChat(@NotNull VoicePlayer player, @NotNull McTextComponent message) {
        if (lastPrint.containsKey(player.getInstance().getUuid())) return;

        player.getInstance().sendMessage(message);
        lastPrint.put(player.getInstance().getUuid(), System.currentTimeMillis());
    }

    private synchronized void sendActionBar(@NotNull VoicePlayer player, @NotNull McTextComponent message) {
        long now = System.currentTimeMillis();
        long last = lastPrint.getOrDefault(player.getInstance().getUuid(), 0L);
        if (now - last > 250L) {
            lastPrint.put(player.getInstance().getUuid(), now);

            player.getInstance().sendActionBar(message);
        }
    }
}
