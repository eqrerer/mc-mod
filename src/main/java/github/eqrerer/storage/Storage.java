package github.eqrerer.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Storage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    public static File getBaseDir() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File base = new File(configDir, "bloxtedstalker");
        if (!base.exists()) base.mkdirs();
        File events = new File(base, "events");
        if (!events.exists()) events.mkdirs();
        return base;
    }

    public static Path getEventsDir() {
        return getBaseDir().toPath().resolve("events");
    }

    public static void writeEvent(Event e) {
        String ts = ISO.format(Instant.now()).replace(":", "-");
        String fileName = e.eventType + "-" + ts + ".json";
        Path out = getEventsDir().resolve(fileName);
        try {
            String json = GSON.toJson(e);
            Files.writeString(out, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // helper to create and write directly from maps if you prefer
    public static void writeRaw(Map<String, Object> map) {
        String ts = ISO.format(Instant.now()).replace(":", "-");
        Path out = getEventsDir().resolve("event-" + ts + ".json");
        try {
            String json = GSON.toJson(map);
            Files.writeString(out, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
