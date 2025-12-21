package su.plo.voice.broadcast;

import com.google.common.collect.Maps;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.slib.api.entity.player.McPlayer;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoBaseVoiceServer;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.event.connection.UdpClientConnectedEvent;
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent;
import su.plo.voice.api.server.language.ServerLanguages;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoicePlayerManager;
import su.plo.voice.broadcast.activation.BroadcastActivation;
import su.plo.voice.broadcast.activation.BroadcastWidePrinter;
import su.plo.voice.broadcast.config.BroadcastConfig;
import su.plo.voice.broadcast.state.BroadcastStateStore;
import su.plo.voice.broadcast.state.JsonBroadcastStateStore;
import su.plo.voice.proto.data.audio.codec.opus.OpusDecoderInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

public abstract class BroadcastAddon implements AddonInitializer {

    private static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    protected final Map<UUID, ServerBroadcastSource> sourceByPlayerId = Maps.newConcurrentMap();

    @Getter
    protected BroadcastConfig config;
    @Getter
    protected BroadcastStateStore stateStore;

    protected BroadcastActivation broadcastActivation;
    protected BroadcastWidePrinter broadcastWidePrinter;

    @InjectPlasmoVoice
    private PlasmoBaseVoiceServer voiceServer;

    @EventSubscribe
    public void onPlayerJoin(@NotNull UdpClientConnectedEvent event) {
        VoicePlayer voicePlayer = event.getConnection().getPlayer();
        McPlayer player = voicePlayer.getInstance();

        stateStore.getByPlayerId(player.getUuid()).ifPresent((state) -> {
            SourceResult result = initializeBroadcastSource(voicePlayer, state.type(), state.arguments());

            if (result != SourceResult.SUCCESS) {
                stateStore.remove(voicePlayer.getInstance().getUuid());
            }
        });
    }

    @EventSubscribe
    public void onPlayerQuit(@NotNull UdpClientDisconnectedEvent event) {
        removeBroadcastSource(event.getConnection().getPlayer().getInstance().getUuid());
    }

    protected synchronized void loadConfig(@NotNull String languageFolder) {
        File addonFolder = new File(voiceServer.getMinecraftServer().getConfigsFolder(), "pv-addon-broadcast");
        addonFolder.mkdirs();

        try {
            File configFile = new File(addonFolder, "config.toml");

            this.config = toml.load(BroadcastConfig.class, configFile, false);
            toml.save(BroadcastConfig.class, config, configFile);

            ServerLanguages languages = voiceServer.getLanguages();
            languages.register(
                    URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL(),
                    languageFolder + "/broadcast.toml",
                    (resourcePath) -> getLanguageResource(languageFolder, resourcePath),
                    new File(addonFolder, "languages")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config", e);
        }

        try {
            this.stateStore = new JsonBroadcastStateStore(
                    voiceServer.getBackgroundExecutor(),
                    new File(addonFolder, "states.json")
            );
            stateStore.load();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load broadcast state store", e);
        }

        if (broadcastActivation == null) {
            this.broadcastWidePrinter = new BroadcastWidePrinter(this);

            this.broadcastActivation = new BroadcastActivation(
                    voiceServer,
                    this,
                    broadcastWidePrinter
            );
            broadcastActivation.register();

            voiceServer.getEventBus().register(this, broadcastActivation);
            voiceServer.getEventBus().register(this, broadcastWidePrinter);
        }
    }

    public void removeBroadcastSource(@NotNull UUID playerId) {
        ServerBroadcastSource source = sourceByPlayerId.remove(playerId);
        if (source != null) source.remove();
    }

    public Optional<ServerBroadcastSource> getBroadcastSource(@NotNull VoicePlayer player, boolean initializeDefault) {
        ServerBroadcastSource broadcastSource = sourceByPlayerId.get(player.getInstance().getUuid());
        if (broadcastSource != null) return Optional.of(broadcastSource);

        if (!initializeDefault || getDefaultSourceType() == null) return Optional.empty();

        SourceResult result = initializeBroadcastSource(player, getDefaultSourceType(), Collections.emptyList());
        if (result != SourceResult.SUCCESS) {
            throw new IllegalStateException("Failed to initialize default broadcast source: " + result);
        }

        return getBroadcastSource(player, false);
    }

    public ServerBroadcastSource getBroadcastSource(
            @NotNull VoicePlayer player,
            @NotNull Consumer<ServerBroadcastSource> builder
    ) {
        return Optional.ofNullable(sourceByPlayerId.get(player.getInstance().getUuid()))
                .orElseGet(() -> createBroadcastSource(builder));
    }

    private ServerBroadcastSource createBroadcastSource(@NotNull Consumer<ServerBroadcastSource> builder) {
        if (broadcastActivation.getSourceLine() == null)
            throw new IllegalStateException("Broadcast source line is not initialized");

        return broadcastActivation.getSourceLine().createBroadcastSource(false, new OpusDecoderInfo(), builder);
    }

    private InputStream getLanguageResource(@NotNull String languageFolder,
                                            @NotNull String resourcePath) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(String.format("broadcast/%s/%s", languageFolder, resourcePath));
    }

    public abstract Optional<McTextComponent> getCurrentBroadcastWideMessage(@NotNull VoicePlayer player);

    public abstract SourceResult initializeBroadcastSource(@NotNull VoicePlayer player,
                                                                     @NotNull String type,
                                                                     @NotNull List<String> arguments);

    public abstract VoicePlayerManager<?> getPlayerManager();

    protected abstract @Nullable String getDefaultSourceType();
}
