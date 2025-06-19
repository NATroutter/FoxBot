package fi.natroutter.foxbot.feature.parties;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.PartyEntry;
import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.feature.parties.data.SettingChange;
import fi.natroutter.foxbot.feature.parties.data.SlowMode;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.ILogData;
import fi.natroutter.foxlib.logger.types.LogData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class PartyHandler {

    //TODO add blacklist config for names!

    private Config config = FoxBot.getConfig().get();
    private MongoHandler mongo = FoxBot.getMongo();
    private FoxLogger logger = FoxBot.getLogger();
    private BotHandler bot = FoxBot.getBotHandler();

    private final List<Long> deleteCycle = new ArrayList<>();

    public PartyHandler() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!bot.isRunning()) return; //TODO lisää systeemi joka tarkistraa kanavat kategorian alta jos database ja categoria menee epö synciin

                //Prune old party channels that are not used anymore
                mongo.getParties().getCollection(collection->{

                    FindIterable<PartyEntry> activeParties = collection.find(Filters.ne("channelID", 0));

                    JDA jda = bot.getJDA();

                    for (PartyEntry party : activeParties) {
                        for (Guild guild : jda.getGuilds()) {

                            VoiceChannel voice = guild.getVoiceChannelById(party.getChannelID());
                            if (voice == null) {
                                logger.error("Failed to cleanup party channels on guild ("+guild.getId()+") on channel ("+party.getChannelID()+") : Channel does not exists?");
                                party.setChannelID(0);
                                party.setPanelID(0);
                                mongo.save(party);
                                continue;
                            }

                            long partyID = voice.getIdLong();

                            //Double check if the target channel is not in party category!
                            if (voice.getParentCategoryIdLong() != config.getParty().getPartyCategory()) continue;

                            //Check if party is empty
                            if (!voice.getMembers().isEmpty()) {
                                if (deleteCycle.contains(partyID)) {
                                    deleteCycle.remove(partyID);
                                    logger.warn("Party channel "+voice.getName()+" ("+voice.getId()+") has been removed from delete cycle : Member joined!");
                                }
                                continue;
                            }

                            if (deleteCycle.contains(partyID)) {
                                deleteChannel(voice, "Empty party channel!");
                                logger.warn("Party channel "+voice.getName()+" ("+voice.getId()+") has been deleted!");
                            } else {
                                deleteCycle.add(partyID);
                                logger.warn("Party channel "+voice.getName()+" ("+voice.getId()+") has been marked to be deleted in next cycle!");
                            }
                        }

                    }
                });

            }
        }, 0, 1000 * 1 );//Every 30 Min   --- // every 12h --->  }, 0, 1000 * 60 * 60 * 12);
    }

    //This needs to be called soon as the bot is connected to server!
    public void connected(JDA jda) {
        //Set "New party" channel permissions!
        for (Guild guild : jda.getGuilds()) {
            long newPartyChannel = config.getParty().getNewPartyChannel();
            VoiceChannel newVoice = guild.getVoiceChannelById(newPartyChannel);
            if (newVoice != null) {
                newVoice.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.VOICE_SPEAK).queue();
            }
        }
    }

    public void createPartyChannel(Guild guild, Category category, Member member) {

        //Create new voice!
        mongo.getParties().findByID(member.getId(), (party)->{
            VoiceChannel chan = guild.getVoiceChannelById(party.getChannelID());

            String partyName;

            if (party.getName() != null && !party.getName().isEmpty() && !party.getName().isBlank()){
                partyName = party.getName();
            } else {
                partyName = (party.getName() != null && !party.getName().isEmpty()) ? party.getName() : createPartyName(member);;
            }

            //If user does not have party channel create new
            if (chan == null) {
                logger.info("Creating new party channel!",
                    new LogMember(member)
                );
                category.createVoiceChannel(partyName).queue(channel -> {

                    //Setup permissions for the party owner
                    setupPermissions(channel, member, true, ()-> {

                        //Setup @everyone role permissions for the party owner
                        setupEveryonePermissions(guild, channel, party.isHidden(), party.isPublicAccess(), ()->{

                            //Move the party owner to new party channel
                            guild.moveVoiceMember(member, channel).queue();

                            //Setup permissions for the party members
                            party.getMembers().forEach(partyMember -> {
                                if (partyMember.getId() != 0) {
                                    Member pMember= guild.getMemberById(partyMember.getId());
                                    if (pMember != null) {
                                        setupPermissions(channel, pMember, partyMember.isAdmin(), ()-> {}, (e)-> {});
                                    }
                                }
                            });

                            //Save current party data to database
                            party.setChannelID(channel.getIdLong());
                            party.setOwnerID(member.getId());
                            party.setName(partyName);
                            mongo.save(party);

                            //Setup channel settingsa
                            Region region = Region.fromKey(party.getRegion());
                            region = (region == Region.UNKNOWN) ? Region.AUTOMATIC : region;
                            channel.getManager()
                                    .setNSFW(party.isNswf())
                                    .setUserLimit(party.getUserLimit())
                                    .setBitrate(party.getBitRate() * 1000)
                                    .setRegion(region)
                                    .setSlowmode(party.getSlowMode())
                                    .queue(success -> {
                                        //Send control panel to integrated text channel
                                        sendControlPanel(guild, channel, party);
                                    });
                        });
                    }, (e)->{});
                });

            //User already has party channel - move user to old party channel
            } else {
                logger.info("Moving user to existing party channel!",
                        new LogMember(member)
                );
                guild.moveVoiceMember(member, chan).queue();
            }
        });
    }

    public List<MessageEmbed> getPanel(Guild guild, VoiceChannel channel, PartyEntry party) {
        List<MessageEmbed> embeds = new ArrayList<>();
        EmbedBuilder panel = FoxFrame.embedTemplate();

        String userLimit = (channel.getUserLimit() > 0) ? String.valueOf(channel.getUserLimit()) : "No user limit";

        Member partyOwner = guild.getMemberById(party.getOwnerID());
        String partyOwnerName = (partyOwner != null) ? partyOwner.getUser().getAsMention() : "Unknown";

        String visibility = party.isHidden() ? "Hidden" : "Visible";
        String publicAccess = party.isPublicAccess() ? "Public" : "Private";
        int bitrate = channel.getBitrate() / 1000;

        Region region = channel.getRegion();

        String slowMode;
        if (channel.getSlowmode() > 0) {
            SlowMode mode = SlowMode.fromValue(channel.getSlowmode());
            slowMode = (mode == null) ? "Unknown" : mode.getArg();
        } else {
            slowMode = "Not Active";
        }

        String nsfw = channel.isNSFW() ? "Yes" : "No";

        panel.setDescription(
                "## \uD83D\uDD08 Control Panel • " + Utils.cutStringEndDots(channel.getName(), 20) + "\n"+
                        "### \uD83D\uDC4B **Welcome to your new Party channel!**\n\n" +
                        "*Here you can customize your channel however you'd like.*\n*You can get started by pressing the \"Display help\" button*\n\n" +
                        "### ⚙️ **Your current settings:**"
        );
        panel.addField("\uD83D\uDC51 Owner", partyOwnerName,true);
        panel.addField("\uD83C\uDFF7️ Channel Name", "```"+channel.getName()+"```",true);
        panel.addField("️\uD83D\uDEA7 User Limit", "```"+userLimit+"```",true);
        panel.addField("\uD83E\uDD77 Visibility", "```"+visibility+"```",true);
        panel.addField("\uD83D\uDD12 Privacy", "```"+publicAccess+"```",true);
        panel.addField("\uD83C\uDFB5 Bitrate", "```"+bitrate+" kbps```",true);
        panel.addField("\uD83D\uDDFA️ Region", "```"+region.getName()+"```",true);
        panel.addField("\uD83D\uDC0C Slowmode", "```"+slowMode+"```",true);
        panel.addField("\uD83D\uDD1E NSFW", "```"+nsfw+"```",true);

        embeds.add(panel.build());

        //Member Panel

        String members = party.getMembers().stream()
                .map(m-> " "+m.getName()+" | "+m.getId()+" | " + (m.isAdmin()?"YES":"NO")+" ")
                .collect(Collectors.joining("\n"));
        if (!members.isBlank()) {
            EmbedBuilder memberPanel = FoxFrame.embedTemplate();
            memberPanel.setDescription(
                    "## \uD83D\uDC65 **Channel Members • "+party.getMembers().size()+"**\n" +
                            "**Format: ** ``Name | UserID | Admin``\n" +
                            "\n" +
                            "```"+members+"```"
            );
            embeds.add(memberPanel.build());
        }
        return embeds;
    }

    public void updatePanel(Guild guild, VoiceChannel channel, PartyEntry party) {
        updatePanel(guild,channel,party, ()->{});
    }

    public void updatePanel(Guild guild, VoiceChannel channel, PartyEntry party, Runnable success) {
        channel.retrieveMessageById(party.getPanelID()).queue(message -> {

            List<MessageEmbed> panel = getPanel(guild, channel, party);

            message.editMessageEmbeds(panel).queue();
            message.editMessageComponents(
                    ActionRow.of(getPanelButtons1()),
                    ActionRow.of(getPanelButtons2()),
                    ActionRow.of(getPanelButtons3())
            ).queue(result-> {
                logger.info("Party control panel updated!",
                        new LogData("PanelID", party.getPanelID()),
                        new LogChannel(channel)
                );
                success.run();
            }, error-> {
                logger.error("Failed to refresh panel data!", error,
                        new LogData("PanelID", party.getPanelID()),
                        new LogChannel(channel)
                );
            });

        }, error->{
            logger.error("Failed to refresh panel data!", error,
                new LogData("PanelID", party.getPanelID()),
                new LogChannel(channel)
            );
        });
    }


    public List<ItemComponent> getPanelButtons1() {
        Button channelRename = Button.secondary("party_channel_rename", "\uD83D\uDCDB Rename");
        Button channelEdit = Button.secondary("party_channel_edit", "✏️ Edit Channel");
        Button displayHelp = Button.secondary("party_channel_help", "\uD83D\uDCDC Display Help");
        return List.of(channelRename,channelEdit,displayHelp);
    }

    public List<ItemComponent> getPanelButtons2() {
        Button channelVisibility = Button.secondary("party_channel_visibility", "\uD83E\uDD77 Change Visibility");
        Button channelPrivacy = Button.secondary("party_channel_privacy", "\uD83D\uDD12 Change Privacy");
        Button channelNsfw = Button.secondary("party_channel_nsfw", "\uD83D\uDD1E Change NSFW");
        return List.of(channelVisibility,channelPrivacy,channelNsfw);
    }

    public List<ItemComponent> getPanelButtons3() {
        Button addMember = Button.success("party_channel_member_add", "➕ Add Member");
        Button removeMember = Button.danger("party_channel_member_remove", "➖ Remove Member");
        Button kickMember = Button.danger("party_channel_member_kick", "\uD83D\uDC5F Kick Member");
        return List.of(addMember,removeMember,kickMember);
    }


    public void sendControlPanel(Guild guild, VoiceChannel channel, PartyEntry partyData) {
        List<MessageEmbed> panels = getPanel(guild,channel,partyData);

        channel.sendMessageEmbeds(panels)
                .addActionRow(getPanelButtons1())
                .addActionRow(getPanelButtons2())
                .addActionRow(getPanelButtons3())
                .queue(message -> {
                    mongo.getParties().findByChannelID(channel.getIdLong(), party-> {
                        if (party == null) {
                            logger.error("Failed to save panelID for party channel because party does not exist!",
                                new LogChannel(channel)
                            );
                            return;
                        }
                        party.setPanelID(message.getIdLong());
                        mongo.save(party);
                    });
                });
    }

    public MessageEmbed helpMessage() {
        EmbedBuilder help = FoxFrame.embedTemplate();
        help.setDescription("## \uD83D\uDCD6 Party Commands\n\n" +
                "•** /party help** - *Displays this help message*\n" +
                "•** /party create** - *Create new party channel*\n" +
                "•** /party disband** - *Delete party channel*\n" +
                "•** /party add-member** - *Add user to channels member list*\n" +
                "•** /party del-member** - *Remove user from channels member list*\n" +
                "•** /party add-admin** - *Add user to channels admin list*\n" +
                "•** /party del-admin** - *Remove user from channels admin list*\n" +
                "\n" +
                "## ❓ What are channel members?\n" +
                "When user has been added to channels member list they can join the channel at anytime but they can not change any settings you also can use the first dropdown in the panel to set who is member of the channel\n" +
                "\n" +
                "## ❓ What are channel admins?\n" +
                "When user has been added to channels admin list they can edit all channels settings in the panel, but they can not add or remove other admins");
        return help.build();
    }

    public void hasPermissions(MessageChannelUnion channel, Member member, Runnable success, BiConsumer<Boolean,String> error) {
        mongo.getParties().findByChannelID(channel.getIdLong(), (party)->{
            if (party == null) {
                logger.error("Failed to check permission on party channel because it does not exists or is invalid!",
                    new LogChannel(channel)
                );
                error.accept(true, "Party channel does not exists or is invalid!");
                return;
            }
            if (member.getId().equals(party.getOwnerID())) {
                success.run();
                return;
            }
            for (PartyEntry.PartyMember partyMember : party.getMembers()) {
                if (partyMember.getId() == member.getIdLong() && partyMember.isAdmin()) {
                    success.run();
                    return;
                }
            }
            error.accept(false, "You do not have permissions for this action!");
        });

    }

    public String createPartyName(Member member) {
        String globalName = member.getUser().getGlobalName();
        if (globalName == null) globalName = "Unknown";

        String suffix = "'s voice";
        int nameLength = 25 - suffix.length();
        return Utils.cutString(globalName, nameLength) + suffix;
    }

    public void deleteChannel(VoiceChannel channel, String reason) {
        long newPartyChannel = config.getParty().getNewPartyChannel();
        if (channel.getIdLong() == newPartyChannel) return;

        channel.delete().reason(reason).queue();

        mongo.getParties().findByChannelID(channel.getIdLong(), (party)->{
            if (party == null) {
                logger.error("Failed to delete party channel from database because party is null!");
                return;
            }
            party.setChannelID(0);
            party.setPanelID(0);
            mongo.save(party);
        });
    }

    public void setupEveryonePermissions(Guild guild, VoiceChannel channel, boolean hidden, boolean publicAccess, Runnable done) {
        List<Permission> allowed = new ArrayList<>();

        allowed.add(Permission.VOICE_SPEAK);
        allowed.add(Permission.VOICE_STREAM);
        allowed.add(Permission.VOICE_USE_VAD);
        allowed.add(Permission.MESSAGE_SEND);
        allowed.add(Permission.MESSAGE_EMBED_LINKS);
        allowed.add(Permission.MESSAGE_ADD_REACTION);
        allowed.add(Permission.MESSAGE_HISTORY);
        if (publicAccess) {
            allowed.add(Permission.VIEW_CHANNEL);
            allowed.add(Permission.VOICE_CONNECT);
        } else {
            if (!hidden) {
                allowed.add(Permission.VIEW_CHANNEL);
            }
        }

        List<Permission> denied = Arrays.stream(Permission.values())
                .filter(e->!allowed.contains(e))
                .toList();

        Role everyoneRole = guild.getPublicRole();
        channel.upsertPermissionOverride(everyoneRole).setPermissions(allowed,denied).queue(s-> done.run());
    }


    public void cleanPermissions(VoiceChannel channel, List<PartyEntry.PartyMember> members, Runnable done) {
        mongo.getParties().findByChannelID(channel.getIdLong(), party-> {
            if (party == null) {
                logger.error("Failed to cleanup overrides on party channel because it does not exists or is invalid!",
                        new LogChannel(channel)
                );
                return;
            }

            List<PermissionOverride> overrides = new ArrayList<>(channel.getPermissionOverrides());
            Set<Long> currentMemberIds = members.stream().map(PartyEntry.PartyMember::getId).collect(Collectors.toSet());

            for (PermissionOverride override : overrides) {
                if (override.isMemberOverride()) {

                    //Prevent channel owner loosing permissions!
                    if (override.getId().equals(party.getOwnerID())) {
                        continue;
                    }

                    if (!currentMemberIds.contains(override.getIdLong())) {
                        ILogData memberLog;
                        Member member = override.getMember();
                        if (member != null) {
                            memberLog = new LogMember(member);
                        } else {
                            memberLog = new LogData("MemberID", override.getId());
                        }

                        override.delete().queue(success->{
                            logger.warn("Removed permission override from party channel",
                                    new LogChannel(channel),
                                    memberLog
                            );
                        }, error->{
                            logger.error("Failed to Removed permission override from party channel",error,
                                    new LogChannel(channel),
                                    memberLog
                            );
                        });
                    }
                }
            }

        });
    }
    public void setupPermissions(VoiceChannel channel, Member member, boolean isChannelAdmin, Runnable done, Consumer<Throwable> failed) {
        //Allowed
        List<Permission> allowed = new ArrayList<>(List.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_SEND_IN_THREADS,
                Permission.CREATE_PUBLIC_THREADS,
                Permission.CREATE_PRIVATE_THREADS,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_EXT_EMOJI,
                Permission.MESSAGE_EXT_STICKER,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_ATTACH_VOICE_MESSAGE,
                Permission.USE_EMBEDDED_ACTIVITIES,
                Permission.USE_APPLICATION_COMMANDS,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_STREAM,
                Permission.VOICE_USE_VAD
        ));
        //Denied
        List<Permission> denied = new ArrayList<>(List.of(
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_PERMISSIONS,
                Permission.MANAGE_WEBHOOKS,
                Permission.MESSAGE_MENTION_EVERYONE,
                Permission.CREATE_INSTANT_INVITE,
                Permission.USE_EXTERNAL_APPLICATIONS,
                Permission.MESSAGE_TTS,
                Permission.MANAGE_EVENTS,
                Permission.CREATE_SCHEDULED_EVENTS,
                Permission.VOICE_USE_SOUNDBOARD,
                Permission.VOICE_USE_EXTERNAL_SOUNDS,
                Permission.PRIORITY_SPEAKER,
                Permission.VOICE_MOVE_OTHERS,
                Permission.VOICE_MUTE_OTHERS,
                Permission.VOICE_DEAF_OTHERS
        ));

        (isChannelAdmin ? allowed : denied).add(Permission.MESSAGE_MANAGE);
        (isChannelAdmin ? allowed : denied).add(Permission.MANAGE_THREADS);
        (isChannelAdmin ? allowed : denied).add(Permission.VOICE_SET_STATUS);
        (isChannelAdmin ? allowed : denied).add(Permission.MESSAGE_SEND_POLLS);

        channel.upsertPermissionOverride(member).setPermissions(allowed,denied).queue(s-> done.run(), failed);
    }

    public void error(IReplyCallback event, String title, String message) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(FoxFrame.getErrorColor());
        eb.setDescription("## \uD83D\uDEA8 "+title+"\n \n"+message+"\n\n*This message will be deleted in 30 seconds!*");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue(this::delayedDelete);
    }

    public void warn(IReplyCallback event, String title, String message) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(FoxFrame.getWarnColor());
        eb.setDescription("## ⚠️ "+title+"\n \n"+message+"\n\n*This message will be deleted in 30 seconds!*");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue(this::delayedDelete);
    }

    public void info(IReplyCallback event, String title, String message) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(FoxFrame.getInfoColor());
        eb.setDescription("## ℹ️ "+title+"\n \n"+message+"\n\n*This message will be deleted in 30 seconds!*");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue(this::delayedDelete);
    }

    public void successHidden(IReplyCallback event, String title, String message, SettingChange... changes) {
        event.replyEmbeds(successEmbed(event,title,message,30,changes)).setEphemeral(true).queue(this::delayedDelete);
    }
    public void success(IReplyCallback event, String title, String message, SettingChange... changes) {
        event.replyEmbeds(successEmbed(event,title,message,60,changes)).queue(m->delayedDelete(m,60));
    }
    public MessageEmbed successEmbed(IReplyCallback event, String title, String message,int deleteDelay, SettingChange... changes) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        Member member = event.getMember();

        eb.setColor(FoxFrame.getSuccessColor());

        eb.setDescription("## ✅ "+title+"\n \n"+message+"\n"+ getChangesBlock(changes)+"\n*This message will be deleted in "+deleteDelay+" seconds!*");

        if (member != null) {
            eb.setFooter("by: " + member.getUser().getName() + " ("+member.getId()+")", member.getUser().getEffectiveAvatarUrl());
        } else {
            eb.setFooter("by: Unknown User");
        }
        return eb.build();
    }

    public String getChangesBlock(SettingChange... changes) {
        List<String> block = new ArrayList<>();
        for (SettingChange change : changes) {
            if (change.getOldValue().equals(change.getNewValue())) {
                continue; // Skip if no change
            }
            block.add(change.getKey() + ": " + change.getOldValue() + " -> " + change.getNewValue());
        }
        if (block.isEmpty()) {
            return "";
        }
        return  "### ✍️ Changes:\n" +
                "```" +
                String.join("\n", block) +
                "```";
    }

    public void delayedDelete(InteractionHook message) {
        delayedDelete(message, 30);
    }
    public void delayedDelete(InteractionHook message, int seconds) {
        // Try to fetch the original message before attempting to delete
        message.retrieveOriginal().queueAfter(seconds,TimeUnit.SECONDS,
            original -> {
                // If retrieval succeeds, schedule deletion
                message.deleteOriginal().queue(
                    success -> {},
                    failure -> {
                        if (failure instanceof ErrorResponseException error) {
                            if (error.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE ||
                                error.getErrorResponse() == ErrorResponse.UNKNOWN_CHANNEL) {
                                return;
                            }
                        }
                        failure.printStackTrace();
                    }
                );
            },
            fetchFailure -> {
                // If the message does not exist, do nothing
                if (fetchFailure instanceof ErrorResponseException error) {
                    if (error.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE ||
                        error.getErrorResponse() == ErrorResponse.UNKNOWN_CHANNEL) {
                        return;
                    }
                }
                fetchFailure.printStackTrace();
            }
        );
    }

    public boolean isBlacklisted(String message) {
        List<String> blacklist = config.getParty().getBlacklistedNames();
        Map<Character, Character> LEET_MAP = Map.ofEntries(
                Map.entry('4', 'a'),
                Map.entry('@', 'a'),
                Map.entry('3', 'e'),
                Map.entry('1', 'i'),
                Map.entry('!', 'i'),
                Map.entry('0', 'o'),
                Map.entry('$', 's'),
                Map.entry('5', 's'),
                Map.entry('7', 't')
        );

        StringBuilder builder = new StringBuilder();
        message = message.toLowerCase();

        for (char c : message.toCharArray()) {
            // Always apply LEET_MAP if present, otherwise keep the char if it's a letter or digit
            if (LEET_MAP.containsKey(c)) {
                builder.append(LEET_MAP.get(c));
            } else if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
            // else: skip non-alphanumeric, non-leet chars (e.g., spaces, punctuation)
        }
        String normalizedMessage = builder.toString();

        for (String word : blacklist) {
            if (normalizedMessage.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}
