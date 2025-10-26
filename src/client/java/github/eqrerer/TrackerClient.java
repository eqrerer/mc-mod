package github.eqrerer;

import github.eqrerer.storage.Event;
import github.eqrerer.storage.Storage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TrackerClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("bloxtedstalker");

    @Override
    public void onInitializeClient() {
        System.out.println("[Bloxted Stalker] Tracker initialized (stdout)");
        LOGGER.info("[Bloxted Stalker] Tracker initialized.");

        // JOIN: explicitly typed lambda matching current Fabric API
        ClientPlayConnectionEvents.JOIN.register((ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) -> {
            try {
                String player = (client.getSession() != null) ? client.getSession().getUsername() : "unknown";
                String server = client.isIntegratedServerRunning()
                        ? "singleplayer"
                        : (client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : "unknown");

                String mcVersion = "unknown";
                try {
                    // use toString() which is stable across mappings; avoids getName() method mismatch
                    mcVersion = SharedConstants.getGameVersion().toString();
                } catch (Throwable t) {
                    // keep "unknown" if anything goes wrong
                }

                Map<String, String> mods = new HashMap<>();
                try {
                    var loader = net.fabricmc.loader.api.FabricLoader.getInstance();
                    for (var mod : loader.getAllMods()) {
                        try {
                            mods.put(mod.getMetadata().getId(), mod.getMetadata().getVersion().toString());
                        } catch (Throwable ignored) { }
                    }
                } catch (Throwable t) {
                    LOGGER.warn("[Bloxted Stalker] Could not enumerate mods: {}", t.toString());
                }

                Event e = new Event();
                e.eventType = "join";
                e.timestamp = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now());
                e.server = server;
                e.player = player;
                e.minecraftVersion = mcVersion;
                e.modVersions = mods;
                e.payload = Map.of("note", "player joined");

                // primary write - should compile and run if Storage and Event are in src/main/java/github/eqrerer/storage
                try{
                    Storage.writeEvent(e);
                } catch (Throwable t) {
                    // fallback: write raw map if signature mismatch happens at runtime/compile-time
                    LOGGER.warn("[Bloxted Stalker] writeEvent failed falling back to  writeRaw: {}", t.toString());
                    Map<String, Object> raw = new HashMap<>();
                    raw.put("eventType", e.eventType);
                    raw.put("timestamp", e.timestamp);
                    raw.put("server", e.server);
                    raw.put("player", e.player);
                    raw.put("minecraftVersion", e.minecraftVersion);
                    raw.put("modVersions", e.modVersions);
                    raw.put("payload", e.payload);
                    Storage.writeRaw(raw);
                }

                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.of("[Bloxted Stalker] Saved join event."), false);
                    }
                });

                LOGGER.info("[Bloxted Stalker] Saved join event for {} @ {}", player, server);
            } catch (Throwable t) {
                LOGGER.error("[Bloxted Stalker] Failed to handle JOIN", t);
            }
        });

        // DISCONNECT handler
        ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayNetworkHandler handler, MinecraftClient client) -> {
            try {
                String player = (client.getSession() != null) ? client.getSession().getUsername() : "unknown";

                Event e = new Event();
                e.eventType = "disconnect";
                e.timestamp = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now());
                e.player = player;
                e.payload = Map.of("note", "player disconnected");

                try {
                    Storage.writeEvent(e);
                } catch (Throwable t) {
                    LOGGER.warn("[Bloxted Stalker] writeEvent failed for disconnect, falling back: {}", t.toString());
                    Map<String, Object> raw = new HashMap<>();
                    raw.put("eventType", e.eventType);
                    raw.put("timestamp", e.timestamp);
                    raw.put("player", e.player);
                    raw.put("payload", e.payload);
                    Storage.writeRaw(raw);
                }

                LOGGER.info("[Bloxted Stalker] Saved disconnect event for {}", player);
            } catch (Throwable t) {
                LOGGER.error("[Bloxted Stalker] Failed to handle DISCONNECT", t);
            }
        });
    }
}
