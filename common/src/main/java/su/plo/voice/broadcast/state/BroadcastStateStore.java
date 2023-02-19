package su.plo.voice.broadcast.state;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface BroadcastStateStore {

    void load() throws Exception;

    void put(@NotNull UUID playerId, @NotNull BroadcastState state);

    void remove(@NotNull UUID playerId);

    Optional<BroadcastState> getByPlayerId(@NotNull UUID playerId);
}
