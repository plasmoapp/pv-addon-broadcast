package su.plo.voice.broadcast.activation;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.lib.api.server.permission.PermissionsManager;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.audio.capture.SelfActivationInfo;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.capture.ServerActivationManager;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.line.ServerSourceLineManager;
import su.plo.voice.api.server.audio.source.ServerDirectSource;
import su.plo.voice.api.server.connection.UdpConnectionManager;
import su.plo.voice.api.server.event.audio.capture.ServerActivationRegisterEvent;
import su.plo.voice.api.server.event.audio.capture.ServerActivationUnregisterEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.event.audio.source.ServerSourcePacketEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.socket.UdpConnection;
import su.plo.voice.broadcast.BroadcastAddon;
import su.plo.voice.proto.data.audio.capture.VoiceActivation;
import su.plo.voice.proto.data.audio.line.VoiceSourceLine;
import su.plo.voice.proto.packets.tcp.clientbound.SourceAudioEndPacket;
import su.plo.voice.proto.packets.tcp.clientbound.SourceInfoPacket;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.Optional;
import java.util.UUID;

public final class BroadcastActivation {

    private static final String ACTIVATION_NAME = "broadcast";

    private final PermissionsManager permissions;

    private final PermissionDefault defaultPermission;
    private final SelfActivationInfo selfActivationInfo;

    private final BroadcastAddon addon;

    private final BroadcastWidePrinter widePrinter;

    @Getter
    private ServerActivation activation;
    @Getter
    private ServerSourceLine sourceLine;

    public BroadcastActivation(@NotNull UdpConnectionManager<? extends VoicePlayer, ? extends UdpConnection> udpConnections,
                               @NotNull PermissionsManager permissions,
                               @NotNull BroadcastAddon addon,
                               @NotNull BroadcastWidePrinter widePrinter) {
        this.permissions = permissions;

        this.defaultPermission = PermissionDefault.OP;
        this.selfActivationInfo = new SelfActivationInfo(udpConnections);

        this.addon = addon;

        this.widePrinter = widePrinter;
    }

    public void register(@NotNull ServerActivationManager activations,
                         @NotNull ServerSourceLineManager sourceLines) {
        ServerActivation.Builder builder = activations.createBuilder(
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
                .build();

        this.sourceLine = sourceLines.register(
                addon,
                ACTIVATION_NAME,
                "pv.activation.broadcast",
                "plasmovoice:textures/icons/speaker_broadcast.png",
                addon.getConfig().sourceLineWeight()
        );
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onActivationRegister(@NotNull ServerActivationRegisterEvent event) {
        ServerActivation activation = event.getActivation();
        if (!activation.getName().equals(ACTIVATION_NAME)) return;

        activation.getPermissions().forEach((permission) ->
                permissions.register(permission, defaultPermission)
        );
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onActivationUnregister(@NotNull ServerActivationUnregisterEvent event) {
        ServerActivation activation = event.getActivation();
        if (!activation.getName().equals(ACTIVATION_NAME)) return;

        activation.getPermissions().forEach(permissions::unregister);
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onSourceSendPacket(@NotNull ServerSourcePacketEvent event) {
        if (!(event.getSource() instanceof ServerDirectSource)) return;

        ServerDirectSource source = (ServerDirectSource) event.getSource();
        if (!source.getSender().isPresent()) return;

        VoicePlayer player = source.getSender().get();
        if (addon.getBroadcastSource(player, false)
                .map((broadcastSource) -> source.equals(broadcastSource.getSource()))
                .orElse(false)) return;

        if (!selfActivationInfo.getLastPlayerActivationIds()
                .containsKey(player.getInstance().getUUID())
        ) {
            return;
        }

        if (event.getPacket() instanceof SourceInfoPacket) {
            selfActivationInfo.updateSelfSourceInfo(
                    player,
                    source,
                    ((SourceInfoPacket) event.getPacket()).getSourceInfo()
            );
        } else if (event.getPacket() instanceof SourceAudioEndPacket) {
            player.sendPacket(event.getPacket());
        }
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerSpeak(@NotNull PlayerSpeakEvent event) {
        if (activation == null) return;

        VoicePlayer player = event.getPlayer();
        PlayerAudioPacket packet = event.getPacket();

        if (!activation.checkPermissions(player)) return;

        getDirectSource(player, packet.getActivationId(), packet.isStereo())
                .ifPresent((source) -> {
                    if (sendAudioPacket(player, source, packet)) {
                        widePrinter.sendMessage(player);
                        event.setCancelled(true);
                    }
                });
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerSpeakEnd(@NotNull PlayerSpeakEndEvent event) {
        VoicePlayer player = event.getPlayer();
        PlayerAudioEndPacket packet = event.getPacket();

        if (!activation.checkPermissions(player)) return;

        getDirectSource(player, packet.getActivationId(), null)
                .ifPresent((source) -> {
                    if (sendAudioEndPacket(source, packet)) event.setCancelled(true);
                });
    }

    private boolean sendAudioPacket(@NotNull VoicePlayer player,
                                    @NotNull ServerDirectSource source,
                                    @NotNull PlayerAudioPacket packet) {
        SourceAudioPacket sourcePacket = new SourceAudioPacket(
                packet.getSequenceNumber(),
                (byte) source.getState(),
                packet.getData(),
                source.getId(),
                (short) 0
        );

        if (source.sendAudioPacket(sourcePacket, packet.getActivationId())) {
            selfActivationInfo.sendAudioInfo(player, source, packet.getActivationId(), sourcePacket);
            return true;
        }

        return false;
    }

    private boolean sendAudioEndPacket(@NotNull ServerDirectSource source,
                                       @NotNull PlayerAudioEndPacket packet) {
        SourceAudioEndPacket sourcePacket = new SourceAudioEndPacket(source.getId(), packet.getSequenceNumber());
        return source.sendPacket(sourcePacket);
    }

    private Optional<ServerDirectSource> getDirectSource(@NotNull VoicePlayer player,
                                                         @NotNull UUID activationId,
                                                         @Nullable Boolean isStereo) {
        if (!activationId.equals(activation.getId())) return Optional.empty();

        return addon.getBroadcastSource(player, true)
                .map((broadcastSource) -> {
                    ServerDirectSource source = broadcastSource.getSource();

                    source.setLine(sourceLine);
                    if (isStereo != null) {
                        source.setStereo(isStereo && activation.isStereoSupported());
                    }

                    return source;
                });
    }
}