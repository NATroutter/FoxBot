package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxframe.bot.BotHandler;
import fi.natroutter.foxframe.permissions.INode;
import fi.natroutter.foxframe.permissions.IPermissionHandler;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.List;

public class FoxBotHandler extends BotHandler {

    @Override
    public String getBotName() {
        return "FoxBot";
    }

    @Override
    public String getVersion() {
        return FoxBot.getVer();
    }

    @Override
    public String getAuthor() {
        return "NATroutter";
    }

    @Override
    public INode getCooldownBypassPerm() {
        return Nodes.BYPASS_COOLDOWN;
    }

    @Override
    public String getToken() {
        return FoxBot.getConfig().get().getToken();
    }

    @Override
    public List<GatewayIntent> getIntents() {
        return List.of(
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_EXPRESSIONS,
                GatewayIntent.SCHEDULED_EVENTS,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.GUILD_WEBHOOKS,
                GatewayIntent.GUILD_INVITES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_MESSAGE_TYPING,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGE_TYPING,
                GatewayIntent.MESSAGE_CONTENT
        );
    }

    @Override
    public IPermissionHandler getPermissionHandler() {
        return FoxBot.getPermissionHandler();
    }
}
