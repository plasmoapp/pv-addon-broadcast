package su.plo.voice.broadcast;

import com.google.common.collect.Maps;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoBaseVoiceServer;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.event.player.PlayerJoinEvent;
import su.plo.voice.api.server.event.player.PlayerQuitEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoicePlayerManager;
import su.plo.voice.broadcast.activation.BroadcastActivation;
import su.plo.voice.broadcast.activation.BroadcastWidePrinter;
import su.plo.voice.broadcast.config.BroadcastConfig;
import su.plo.voice.broadcast.source.BroadcastSource;
import su.plo.voice.broadcast.state.BroadcastStateStore;
import su.plo.voice.broadcast.state.JsonBroadcastStateStore;
import su.plo.voice.proto.data.audio.codec.opus.OpusDecoderInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class BroadcastAddon {

    private static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    protected final Map<UUID, BroadcastSource<?>> sourceByPlayerId = Maps.newConcurrentMap();

    @Getter
    protected BroadcastConfig config;
    @Getter
    protected BroadcastStateStore stateStore;

    protected BroadcastActivation broadcastActivation;

    @EventSubscribe
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        stateStore.getByPlayerId(event.getPlayerId()).ifPresent((state) -> {
            getPlayerManager().getPlayerById(event.getPlayerId()).ifPresent((player) -> {
                BroadcastSource.Result result = initializeBroadcastSource(player, state.type(), state.arguments());

                if (result != BroadcastSource.Result.SUCCESS) {
                    stateStore.remove(player.getInstance().getUUID());
                }
            });
        });
    }

    @EventSubscribe
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        removeBroadcastSource(event.getPlayerId());
    }

    protected synchronized void loadConfig(@NotNull PlasmoBaseVoiceServer voiceServer,
                                           @NotNull String languageFolder) {
        File addonFolder = new File(voiceServer.getConfigFolder(), "addons/broadcast");
        addonFolder.mkdirs();

        try {
            File configFile = new File(addonFolder, "config.toml");

            this.config = toml.load(BroadcastConfig.class, configFile, false);
            toml.save(BroadcastConfig.class, config, configFile);

            voiceServer.getLanguages().register(
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
            BroadcastWidePrinter broadcastWidePrinter = new BroadcastWidePrinter(this);

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
        BroadcastSource<?> source = sourceByPlayerId.remove(playerId);
        if (source != null) source.close();
    }

    public Optional<BroadcastSource<?>> getBroadcastSource(@NotNull VoicePlayer player, boolean initializeDefault) {
        BroadcastSource<?> broadcastSource = sourceByPlayerId.get(player.getInstance().getUUID());
        if (broadcastSource != null) return Optional.of(broadcastSource);

        if (!initializeDefault) return Optional.empty();

        if (initializeBroadcastSource(player, "server", Collections.emptyList()) != BroadcastSource.Result.SUCCESS) {
            throw new IllegalStateException("Failed to initialize default broadcast source");
        }

        return getBroadcastSource(player, false);
    }

    public ServerDirectSource getDirectSource(@NotNull VoicePlayer player) {
        return Optional.ofNullable(sourceByPlayerId.get(player.getInstance().getUUID()))
                .map(BroadcastSource::getSource)
                .orElseGet(this::createDirectSource);
    }

    private ServerDirectSource createDirectSource() {
        if (broadcastActivation.getSourceLine() == null)
            throw new IllegalStateException("Broadcast source line is not initialized");

        return broadcastActivation.getSourceLine().createDirectSource(false, new OpusDecoderInfo());
    }

    private InputStream getLanguageResource(@NotNull String languageFolder,
                                            @NotNull String resourcePath) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(String.format("broadcast/%s/%s", languageFolder, resourcePath));
    }

    public abstract Optional<MinecraftTextComponent> getCurrentBroadcastWideMessage(@NotNull VoicePlayer player);

    public abstract BroadcastSource.Result initializeBroadcastSource(@NotNull VoicePlayer player,
                                                                     @NotNull String type,
                                                                     @NotNull List<String> arguments);

    public abstract VoicePlayerManager<?> getPlayerManager();
}
