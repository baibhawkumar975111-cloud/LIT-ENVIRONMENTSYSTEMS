package com.litteam.environment.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MessageUtil() {
    }

    public static Component parse(String text) {
        return MINI.deserialize(text == null ? "" : text);
    }

    public static Component prefixed(String prefix, String message) {
        return parse(prefix + message);
    }

    /** Same as parse, but without the default italics that item names and lore get. */
    public static Component item(String text) {
        return parse(text).decoration(TextDecoration.ITALIC, false);
    }

    /** Use for anything that isn't ours (world names, for example) before putting it into a MiniMessage string. */
    public static String escape(String text) {
        return MINI.escapeTags(text);
    }
}
