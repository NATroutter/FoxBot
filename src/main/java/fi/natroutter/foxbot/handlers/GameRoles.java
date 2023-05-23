package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.objects.GameRole;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GameRoles {

    private BotHandler botHandler;
    private NATLogger logger = FoxBot.getLogger();



    private String roleIconBase = "https://cdn.nat.gg/img/discord/foxbot/games/";

    public static List<GameRole> roles = List.of(
            new GameRole(
                    "Minecraft",
                    "Vanilla Minecraft",
                    "Role for vanilla minecraft players",
                    Emoji.fromFormatted("<:minecraft:1104752295008280666>"),
                    "minecraft.png"
            ),
            new GameRole("Minecraft_modded",
                    "Modded Minecraft",
                    "Role for modded minecraft players",
                    Emoji.fromFormatted("<:modded_minecraft:1104752292567203870>"),
                    "modded_minecraft.png"
            ),
            new GameRole(
                    "CSGO",
                    "Counter-Strike: Global Offensive",
                    "Role for CSGO players",
                    Emoji.fromFormatted("<:csgo:1104752290495221800>"),
                    "csgo.png"
            ),
            new GameRole(
                    "Overwatch2",
                    "Overwatch 2",
                    "Role for Overwatch 2 players",
                    Emoji.fromFormatted("<:overwatch2:1104752298904797194>"),
                    "overwatch2.png"
            ),
            new GameRole(
                    "Rust",
                    "Rust",
                    "Role for Rust players",
                    Emoji.fromFormatted("<:rust:1104752288746176522>"),
                    "rust.png"
            ),
            new GameRole(
                    "Sot",
                    "Sea of Thieves",
                    "Role for Sea of Thieves players",
                    Emoji.fromFormatted("<:sot:1104752285789212702>"),
                    "sot.png"
            ),
            new GameRole(
                    "TowerUnite",
                    "TowerUnite",
                    "Role for TowerUnite players",
                    Emoji.fromFormatted("<:towerunite:1110097929684910090>"),
                    "towerunite.png"
            )
    );

    public GameRoles(BotHandler botHandler){
        this.botHandler = botHandler;
        for (Guild guild : botHandler.getJda().getGuilds()) {
            logger.info("Checking roles for guild: " + guild.getName());
            for (GameRole gRole : roles) {
                Role role = getRole(guild, gRole);
                if (role == null) {
                    RoleAction rAction = guild.createRole().setName(gRole.tag()).setHoisted(false).setMentionable(true);
                    try {
                        rAction = rAction.setIcon(Icon.from(new URL(roleIconBase + gRole.roleIcon()).openStream()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    rAction.queue();
                    logger.info("  - Role: " + gRole.tag() + " | Created!");
                } else {
                    logger.info("  - Role: " + gRole.tag() + " | Exists!");
                }
            }
        }
    }

    public static GameRole fromString(Guild guild, String name) {
        for(GameRole grole : roles) {
            if (grole.tag().equalsIgnoreCase(name)) {
                return grole;
            }
        }
        return null;
    }

    public static Role getRole(Guild guild, GameRole gRole) {
        return getRole(guild, gRole.tag());
    }

    public static Role getRole(Guild guild, String tag) {
        List<Role> roles = guild.getRolesByName(tag, true);
        if (roles.isEmpty()) return null;
        return roles.get(0);
    }

}
