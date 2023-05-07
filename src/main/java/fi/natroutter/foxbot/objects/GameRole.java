package fi.natroutter.foxbot.objects;

import net.dv8tion.jda.api.entities.emoji.EmojiUnion;

public record GameRole(String tag, String name, String description, EmojiUnion emoji, String roleIcon){};
