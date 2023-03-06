package su.plo.voice.broadcast.server.source;

import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.server.world.MinecraftServerWorld;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.broadcast.source.BroadcastSource;

import java.util.List;

public final class WorldBroadcastSource extends BroadcastSource<VoiceServerPlayer> {

    private final List<MinecraftServerWorld> worlds;

    public WorldBroadcastSource(@NotNull ServerDirectSource source,
                                @NotNull VoiceServerPlayer player,
                                @NotNull List<MinecraftServerWorld> worlds) {
        super(source, player);

        this.worlds = worlds;
        initialize();
    }

    @Override
    public boolean filterPlayer(@NotNull VoiceServerPlayer player) {
        return super.filterPlayer(player) && worlds.contains(player.getInstance().getWorld());
    }
}
