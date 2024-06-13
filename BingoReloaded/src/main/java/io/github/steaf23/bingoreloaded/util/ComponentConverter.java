package io.github.steaf23.bingoreloaded.util;

import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ComponentConverter
{
    public static Component bungeeComponentToAdventure(BaseComponent component) {
        return GsonComponentSerializer.gson().deserialize(ComponentSerializer.toJson(component).toString());
    }
}
