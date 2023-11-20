package su.plo.voice.broadcast.server.source;

import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.source.BroadcastFilter;

public final class RangeBroadcastFilter extends BroadcastFilter<VoiceServerPlayer> {

    private final int distanceSquared;

    private final ServerPos3d playerPosition = new ServerPos3d();
    private final ServerPos3d listenerPosition = new ServerPos3d();

    public RangeBroadcastFilter(@NotNull VoiceServerPlayer player, int distance) {
        super(player);

        this.distanceSquared = distance * distance;
    }

    @Override
    public boolean test(@NotNull VoiceServerPlayer listener) {
        if (!super.test(listener)) return false;

        this.player.getInstance().getServerPosition(playerPosition);
        listener.getInstance().getServerPosition(listenerPosition);

        return listenerPosition.getWorld().equals(playerPosition.getWorld()) &&
                listenerPosition.distanceSquared(playerPosition) <= distanceSquared;
    }
}
