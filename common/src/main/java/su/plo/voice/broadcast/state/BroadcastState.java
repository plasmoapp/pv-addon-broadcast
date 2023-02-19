package su.plo.voice.broadcast.state;

import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@Accessors(fluent = true)
public final class BroadcastState {

    private @NotNull final String type;
    private @NotNull final List<String> arguments;
}
