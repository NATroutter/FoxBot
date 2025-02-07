package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxframe.interfaces.HandlerFrame;
import fi.natroutter.foxframe.permissions.INode;
import fi.natroutter.foxframe.permissions.IPermissionHandler;

public class BotHandler extends HandlerFrame {

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
    public IPermissionHandler getPermissionHandler() {
        return FoxBot.getPermissionHandler();
    }
}
