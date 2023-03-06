package su.plo.voice.broadcast.proxy.source;

import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.proxy.server.MinecraftProxyServerInfo;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.broadcast.source.BroadcastSource;

import java.util.List;

public final class ServerBroadcastSource extends BroadcastSource<VoiceProxyPlayer> {

    private final List<MinecraftProxyServerInfo> servers;

    public ServerBroadcastSource(@NotNull ServerDirectSource source,
                                 @NotNull VoiceProxyPlayer player,
                                 @NotNull List<MinecraftProxyServerInfo> servers) {
        super(source, player);

        this.servers = servers;
        initialize();
    }

    @Override
    public boolean filterPlayer(@NotNull VoiceProxyPlayer player) {
        return super.filterPlayer(player) &&
                player.getInstance().getServer()
                        .map((server) -> servers.contains(server.getServerInfo()))
                        .orElse(false);
    }
}
