package io.github.meyllane.frontiereLock.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;

public class PluginHeader {
    public static Component getPluginHeader() {
        return MiniMessage.miniMessage().deserialize("<color:white>[</color><gradient:#AAE2FF:#F88CFD>SFLock</gradient><color:white>]</color> ");
    }

    public static Component getPluginErrorMessage(String errorMessage) {
        Component message = Component.text(errorMessage).color(TextColor.fromHexString("#D13D3D"));
        return PluginHeader.getPluginHeader().append(message);
    }

    public static Component getPluginSuccessMessage(String successMessage) {
        Component message = Component.text(successMessage).color(TextColor.fromHexString("#7CE65E"));
        return PluginHeader.getPluginHeader().append(message);
    }

    public static Component getPluginInfoMessage(String infoMessage) {
        Component message = Component.text(infoMessage).color(TextColor.fromHexString("#40F0F5"));
        return PluginHeader.getPluginHeader().append(message);
    }
}
