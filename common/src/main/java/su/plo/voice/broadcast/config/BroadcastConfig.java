package su.plo.voice.broadcast.config;

import lombok.Data;
import lombok.experimental.Accessors;
import su.plo.config.Config;
import su.plo.config.ConfigField;

@Config
@Data
@Accessors(fluent = true)
public final class BroadcastConfig {

    @ConfigField(comment = "Available values:\n" +
            "ACTION_BAR - show current broadcast-wide in action bar\n" +
            "CHAT - show current broadcast-wide in chat\n" +
            "HIDDEN - don't show current broadcast-wide")
    private ShowCurrentBroadcastWide showCurrentBroadcastWide = ShowCurrentBroadcastWide.ACTION_BAR;

    @ConfigField
    private int activationWeight = 12;
    @ConfigField
    private int sourceLineWeight = 12;

    public enum ShowCurrentBroadcastWide {
        ACTION_BAR,
        CHAT,
        HIDDEN
    }
}
