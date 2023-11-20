package su.plo.voice.broadcast.source;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.server.player.VoicePlayer;

import java.util.function.Predicate;

@RequiredArgsConstructor
public abstract class BroadcastFilter<P extends VoicePlayer> implements Predicate<P> {

    @Getter
    protected final P player;

    @Override
    public boolean test(@NotNull P player) {
        return !this.player.equals(player);
    }
}
