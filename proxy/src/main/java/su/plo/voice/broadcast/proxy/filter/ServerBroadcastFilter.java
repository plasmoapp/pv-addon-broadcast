package su.plo.voice.broadcast.proxy.filter;

import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.proxy.server.McProxyServerInfo;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.broadcast.source.BroadcastFilter;

import java.util.List;
import java.util.Optional;

public class ServerBroadcastFilter extends BroadcastFilter<VoiceProxyPlayer> {

    private final List<McProxyServerInfo> servers;

    public ServerBroadcastFilter(@NotNull VoiceProxyPlayer player, @NotNull List<McProxyServerInfo> servers) {
        super(player);

        this.servers = servers;
    }

    @Override
    public boolean test(@NotNull VoiceProxyPlayer player) {
        return super.test(player) &&
                Optional.ofNullable(player.getInstance().getServer())
                        .map((server) -> servers.contains(server.getServerInfo()))
                        .orElse(false);
    }
}
