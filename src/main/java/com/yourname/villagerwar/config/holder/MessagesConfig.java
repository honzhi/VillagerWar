package com.yourname.villagerwar.config.holder;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessagesConfig {

    private final Map<String, String> messages;

    public MessagesConfig(ConfigurationSection section) {
        this.messages = new HashMap<>();
        if (section == null) return;
        for (String key : section.getKeys(true)) {
            if (section.isString(key)) {
                messages.put(key, section.getString(key));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "<missing: " + key + ">");
    }

    public String getMessage(String key, String def) {
        return messages.getOrDefault(key, def);
    }

    public Map<String, String> getAllMessages() {
        return Collections.unmodifiableMap(messages);
    }
}
