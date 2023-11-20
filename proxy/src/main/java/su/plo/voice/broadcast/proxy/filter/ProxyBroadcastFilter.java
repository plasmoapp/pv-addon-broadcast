package su.plo.voice.broadcast.proxy.filter;

import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.broadcast.source.BroadcastFilter;

public class ProxyBroadcastFilter extends BroadcastFilter<VoiceProxyPlayer> {

    public ProxyBroadcastFilter(@NotNull VoiceProxyPlayer player) {
        super(player);
    }
}
