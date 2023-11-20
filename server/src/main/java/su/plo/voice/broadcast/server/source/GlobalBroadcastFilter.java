package su.plo.voice.broadcast.server.source;

import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.source.BroadcastFilter;

public final class GlobalBroadcastFilter extends BroadcastFilter<VoiceServerPlayer> {

    public GlobalBroadcastFilter(@NotNull VoiceServerPlayer player) {
        super(player);
    }
}
