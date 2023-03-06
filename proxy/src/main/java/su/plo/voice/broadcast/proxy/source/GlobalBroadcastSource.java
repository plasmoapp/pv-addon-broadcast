package su.plo.voice.broadcast.proxy.source;

import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.proxy.player.VoiceProxyPlayer;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.broadcast.source.BroadcastSource;

public final class GlobalBroadcastSource extends BroadcastSource<VoiceProxyPlayer> {

    public GlobalBroadcastSource(@NotNull ServerDirectSource source,
                                 @NotNull VoiceProxyPlayer player) {
        super(source, player);
        initialize();
    }
}
