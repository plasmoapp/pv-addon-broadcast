package su.plo.voice.broadcast.activation;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.slib.api.permission.PermissionDefault;
import su.plo.voice.api.server.PlasmoBaseVoiceServer;
import su.plo.voice.api.server.audio.capture.PlayerActivationInfo;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.BaseServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.proto.packets.tcp.clientbound.SourceAudioEndPacket;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.Optional;
import java.util.UUID;

public final class BroadcastActivation {

    private static final String ACTIVATION_NAME = "broadcast";

    private final PlasmoBaseVoiceServer voiceServer;

    private final BroadcastAddon addon;

    private final BroadcastWidePrinter widePrinter;

    @Getter
    private ServerActivation activation;
    @Getter
    private BaseServerSourceLine sourceLine;

    public BroadcastActivation(
            @NotNull PlasmoBaseVoiceServer voiceServer,
            @NotNull BroadcastAddon addon,
            @NotNull BroadcastWidePrinter widePrinter
    ) {
        this.voiceServer = voiceServer;

        this.addon = addon;

        this.widePrinter = widePrinter;
    }

    public void register() {
        ServerActivation.Builder builder = voiceServer.getActivationManager().createBuilder(
                addon,
                ACTIVATION_NAME,
                "pv.activation.broadcast",
                "plasmovoice:textures/icons/microphone_broadcast.png",
                "pv.activation.broadcast",
                addon.getConfig().activationWeight()
        );
        this.activation = builder
                .setProximity(false)
                .setTransitive(false)
                .setStereoSupported(true)
                .setPermissionDefault(PermissionDefault.OP)
                .addPermission("pv.addon.broadcast.*")
                .build();
        activation.onPlayerActivation(this::onActivation);
        activation.onPlayerActivationEnd(this::onActivationEnd);

        this.sourceLine = voiceServer.getSourceLineManager().createBuilder(
                addon,
                ACTIVATION_NAME,
                "pv.activation.broadcast",
                "plasmovoice:textures/icons/speaker_broadcast.png",
                addon.getConfig().sourceLineWeight()
        ).build();
    }

    private ServerActivation.Result onActivation(@NotNull VoicePlayer player, @NotNull PlayerAudioPacket packet) {
        return getBroadcastSource(player, packet.getActivationId(), packet.isStereo())
                .map((source) -> {
                    if (sendAudioPacket(player, source, packet)) {
                        widePrinter.sendMessage(player);
                        return ServerActivation.Result.HANDLED;
                    }

                    return ServerActivation.Result.IGNORED;
                })
                .orElse(ServerActivation.Result.IGNORED);
    }

    public ServerActivation.Result onActivationEnd(@NotNull VoicePlayer player, @NotNull PlayerAudioEndPacket packet) {
        return getBroadcastSource(player, packet.getActivationId(), null)
                .map((source) -> {
                    if (sendAudioEndPacket(source, packet))
                        return ServerActivation.Result.HANDLED;
                    return ServerActivation.Result.IGNORED;
                })
                .orElse(ServerActivation.Result.IGNORED);
    }

    private boolean sendAudioPacket(
            @NotNull VoicePlayer player,
            @NotNull ServerBroadcastSource source,
            @NotNull PlayerAudioPacket packet
    ) {
        SourceAudioPacket sourcePacket = new SourceAudioPacket(
                packet.getSequenceNumber(),
                (byte) source.getState(),
                packet.getData(),
                source.getId(),
                (short) 0
        );

        return source.sendAudioPacket(sourcePacket, new PlayerActivationInfo(player, packet));
    }

    private boolean sendAudioEndPacket(@NotNull ServerBroadcastSource source,
                                       @NotNull PlayerAudioEndPacket packet) {
        SourceAudioEndPacket sourcePacket = new SourceAudioEndPacket(source.getId(), packet.getSequenceNumber());
        return source.sendPacket(sourcePacket);
    }

    private Optional<ServerBroadcastSource> getBroadcastSource(@NotNull VoicePlayer player,
                                                               @NotNull UUID activationId,
                                                               @Nullable Boolean isStereo) {
        if (!activationId.equals(activation.getId())) return Optional.empty();

        return addon.getBroadcastSource(player, true)
                .map((source) -> {
                    if (isStereo != null) {
                        source.setStereo(isStereo && activation.isStereoSupported());
                    }

                    return source;
                });
    }
}
