package su.plo.voice.broadcast.source;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.server.audio.source.BaseServerSourceManager;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.player.VoicePlayer;

@RequiredArgsConstructor
public abstract class BroadcastSource<P extends VoicePlayer> {

    protected final BaseServerSourceManager sourceManager;
    @Getter
    protected final ServerDirectSource source;
    @Getter
    protected final P player;

    public void initialize() {
        source.clearFilters();
        source.addFilter((player) -> filterPlayer((P) player));
        source.setSender(player);
    }

    public boolean filterPlayer(@NotNull P player) {
        return !this.player.equals(player);
    }

    public void close() {
        sourceManager.remove(source.getId());
    }

    public enum Result {

        SUCCESS,
        NO_PERMISSION,
        BAD_ARGUMENTS,
        UNKNOWN
    }
}
