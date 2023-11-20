package su.plo.voice.broadcast.server.source;

import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.source.BroadcastFilter;

import java.util.List;

public final class WorldBroadcastFilter extends BroadcastFilter<VoiceServerPlayer> {

    private final List<McServerWorld> worlds;

    public WorldBroadcastFilter(
            @NotNull VoiceServerPlayer player,
            @NotNull List<McServerWorld> worlds
    ) {
        super(player);
        this.worlds = worlds;
    }

    @Override
    public boolean test(@NotNull VoiceServerPlayer player) {
        return super.test(player) && worlds.contains(player.getInstance().getWorld());
    }
}
