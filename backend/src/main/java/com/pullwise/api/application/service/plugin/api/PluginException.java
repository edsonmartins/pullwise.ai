package com.pullwise.api.application.service.plugin.api;

/**
 * Exceção lançada quando ocorre um erro no plugin.
 */
public class PluginException extends Exception {

    private final String pluginId;

    public PluginException(String pluginId, String message) {
        super(message);
        this.pluginId = pluginId;
    }

    public PluginException(String pluginId, String message, Throwable cause) {
        super(message, cause);
        this.pluginId = pluginId;
    }

    public PluginException(String message) {
        this(null, message);
    }

    public PluginException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public String getPluginId() {
        return pluginId;
    }
}
