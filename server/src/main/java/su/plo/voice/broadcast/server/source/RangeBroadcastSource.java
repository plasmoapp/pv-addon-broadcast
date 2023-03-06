package su.plo.voice.broadcast.server.source;

import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.server.world.ServerPos3d;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.source.BroadcastSource;

public final class RangeBroadcastSource extends BroadcastSource<VoiceServerPlayer> {

    private final int distanceSquared;

    private final ServerPos3d playerPosition = new ServerPos3d();
    private final ServerPos3d connectionPosition = new ServerPos3d();

    public RangeBroadcastSource(@NotNull ServerDirectSource source,
                                @NotNull VoiceServerPlayer player,
                                int distance) {
        super(source, player);

        this.distanceSquared = distance * distance;
        initialize();
    }

    @Override
    public boolean filterPlayer(@NotNull VoiceServerPlayer player) {
        if (!super.filterPlayer(player)) return false;

        this.player.getInstance().getServerPosition(playerPosition);
        player.getInstance().getServerPosition(connectionPosition);

        return connectionPosition.getWorld().equals(playerPosition.getWorld()) &&
                connectionPosition.distanceSquared(playerPosition) <= distanceSquared;
    }
}
