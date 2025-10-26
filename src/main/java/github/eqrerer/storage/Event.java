package github.eqrerer.storage;

import java.util.Map;

public class Event {
    public String eventType;
    public String timestamp;
    public String server;
    public String player;
    public String minecraftVersion;
    public Map<String, String> modVersions;
    public Map<String, Object> payload;

    public Event() {}
}
