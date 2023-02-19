package su.plo.voice.broadcast.state;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

@RequiredArgsConstructor
public final class JsonBroadcastStateStore implements BroadcastStateStore {

    private static final Gson GSON = new Gson();
    private static final Type STATES_MAP_TYPE = new TypeToken<Map<UUID, BroadcastState>>() {
    }.getType();

    private final ScheduledExecutorService backgroundExecutor;
    private final File file;

    private final Map<UUID, BroadcastState> stateByPlayerId = Maps.newConcurrentMap();

    @Override
    public void load() throws Exception {
        if (!file.exists()) return;

        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        Map<UUID, BroadcastState> data = GSON.fromJson(bufferedReader, STATES_MAP_TYPE);

        stateByPlayerId.putAll(data);
    }

    @Override
    public void put(@NotNull UUID playerId, @NotNull BroadcastState state) {
        stateByPlayerId.put(playerId, state);
        backgroundExecutor.execute(this::save);
    }

    @Override
    public void remove(@NotNull UUID playerId) {
        stateByPlayerId.remove(playerId);
        backgroundExecutor.execute(this::save);
    }

    @Override
    public Optional<BroadcastState> getByPlayerId(@NotNull UUID playerId) {
        return Optional.ofNullable(stateByPlayerId.get(playerId));
    }

    private void save() {
        try (Writer writer = new FileWriter(file)) {
            writer.write(GSON.toJson(stateByPlayerId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
