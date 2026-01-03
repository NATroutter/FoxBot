package fi.natroutter.foxbot;

import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.commands.party.PartyCreateCommand;
import fi.natroutter.foxbot.commands.party.PartyDisbandCommand;
import fi.natroutter.foxbot.commands.party.PartyRenameCommand;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.feature.EventLogger;
import fi.natroutter.foxbot.feature.InviteTracker;
import fi.natroutter.foxbot.feature.SpamListener;
import fi.natroutter.foxbot.feature.socialcredit.listeners.SocialMessageReceiveListener;
import fi.natroutter.foxbot.feature.socialcredit.listeners.SocialMessageUpdateListener;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.feature.parties.listeners.CreatePartyListener;
import fi.natroutter.foxbot.feature.parties.listeners.PartyButtonListener;
import fi.natroutter.foxbot.feature.parties.listeners.PartyModalListener;
import fi.natroutter.foxframe.bot.DiscordBot;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxframe.permissions.INode;
import fi.natroutter.foxframe.permissions.IPermissionHandler;
import fi.natroutter.foxframe.permissions.PermissionHolder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;

public class BotHandler extends DiscordBot {

    private static Config config = FoxBot.getConfigProvider().get();

    @Override
    public String name() {
        return "FoxBot";
    }

    @Override
    public Activity activity() {
        return Activity.watching("\uD83D\uDC40 Your behavior");
    }

    @Override
    public String version() {
        return FoxBot.getVer();
    }

    @Override
    public String author() {
        return "NATroutter";
    }

    @Override
    public String token() {
        return FoxBot.getConfigProvider().get().getToken();
    }

    @Override
    public IPermissionHandler permissionHandler() {
        return FoxBot.getPermissionHandler();
    }

    @Override
    public PermissionHolder permissionHolder() {
        return new PermissionHolder() {
            @Override
            public INode bypassCommandCooldown() {
                return Nodes.BYPASS_COOLDOWN_COMMAND;
            }

            @Override
            public INode bypassButtonCooldown() {
                return Nodes.BYPASS_COOLDOWN_BUTTON;
            }
        };
    }

    @Override
    public List<DiscordCommand> commands() {
        List<DiscordCommand> commands = new ArrayList<>(List.of(
                new Prune(),
                new Permission(),
                new Batroutter(),
                new About(),
                new Info(),
                new CoinFlip(),
                new Dice(),
                new Ask(),
                new Yiff(),
                new Embed(),
                new Wakeup(),
                new Pick(),
                new Fox(),
                new SocialCredit(),
                new Invites(),
                new Catify(),
                new Grammar()
        ));
        if (config.getParty().isEnabled()) {
            commands.addAll(List.of(
                    new PartyCreateCommand(),
                    new PartyDisbandCommand(),
                    new PartyRenameCommand()
            ));
        }
        if (config.getZipline().isEnabled()) {
            commands.add(new Shorten());
        }
        return commands;
    }

    @Override
    public List<ListenerAdapter> listener() {
        List<ListenerAdapter> listeners = new ArrayList<>(List.of(
                new EventLogger(),

                new SocialMessageReceiveListener(),
                new SocialMessageUpdateListener(),

                new SpamListener(),
                new InviteTracker()
        ));

        if (config.getParty().isEnabled()) {
            listeners.addAll(List.of(
                    new CreatePartyListener(),
                    new PartyButtonListener(),
                    new PartyModalListener()
            ));
        }

        return listeners;
    }


}
