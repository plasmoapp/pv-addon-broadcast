package su.plo.voice.broadcast.server.source;

import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.audio.source.ServerSourceManager;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.source.BroadcastSource;

public final class GlobalBroadcastSource extends BroadcastSource<VoiceServerPlayer> {

    public GlobalBroadcastSource(@NotNull ServerSourceManager sourceManager,
                                 @NotNull ServerDirectSource source,
                                 @NotNull VoiceServerPlayer player) {
        super(sourceManager, source, player);
        initialize();
    }
}
