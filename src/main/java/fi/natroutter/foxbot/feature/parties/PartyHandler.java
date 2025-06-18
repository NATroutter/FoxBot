package fi.natroutter.foxbot.feature.parties;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.PartyEntry;
import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.logger.FoxLogger;
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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class PartyHandler {

    private Config config = FoxBot.getConfig().get();
    private MongoHandler mongo = FoxBot.getMongo();
    private FoxLogger logger = FoxBot.getLogger();
    private BotHandler botHandler = FoxBot.getBotHandler();

    private final List<Long> deleteCycle = new ArrayList<>();

    public PartyHandler() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!botHandler.isRunning()) return; //TODO lisää systeemi joka tarkistraa kanavat kategorian alta jos database ja categoria menee epö synciin

                //Prune old party channels that are not used anymore
                mongo.getParties().getCollection(collection->{

                    FindIterable<PartyEntry> activeParties = collection.find(Filters.ne("channelID", 0));

                    JDA jda = botHandler.getJDA();

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
                partyName = (party.getName() != null && !party.getName().isEmpty()) ? party.getName() : getNewPartyName(member);;
            }

            //If user does not have party channel create new
            if (chan == null) {
                logger.info("Creating new party channel for "+ FoxFrame.getLogName(member));
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
                                        setupPermissions(channel, pMember, partyMember.isAdmin(), ()-> {});
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
                    });
                });

            //User already has party channel - move user to old party channel
            } else {
                logger.info("Moving user "+ FoxFrame.getLogName(member) + " for existing party channel");
                guild.moveVoiceMember(member, chan).queue();
            }
        });
    }

    public List<MessageEmbed> getPanel(Guild guild, VoiceChannel channel, PartyEntry partyData) {
        List<MessageEmbed> embeds = new ArrayList<>();
        EmbedBuilder panel = FoxFrame.embedTemplate();

        String userLimit = (channel.getUserLimit() > 0) ? String.valueOf(channel.getUserLimit()) : "No user limit";

        Member partyOwner = guild.getMemberById(partyData.getOwnerID());
        String partyOwnerName = (partyOwner != null) ? partyOwner.getUser().getAsMention() : "Unknown";

        String visibility = partyData.isHidden() ? "Hidden" : "Visible";
        String publicAccess = partyData.isPublicAccess() ? "Public" : "Private";
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
                "## Control Panel • \uD83D\uDD08 " + Utils.cutStringEndDots(channel.getName(), 20) + "\n"+
                        "### \uD83D\uDC4B **Welcome to your new Party channel!**\n\n" +
                        "*Here you can customize your channel however you'd like.*\n*You can get started by pressing the \"Display help\" button*\n\n" +
                        "### ⚙️ **Your current settings:**"
        );
        panel.addField("\uD83D\uDC51 Owner", partyOwnerName,true);
        panel.addField("\uD83C\uDFF7️ Channel Name", "```"+channel.getName()+"```",true);
        panel.addField("\uD83D\uDC65️ User Limit", "```"+userLimit+"```",true);
        panel.addField("\uD83E\uDD77 Visibility", "```"+visibility+"```",true);
        panel.addField("\uD83D\uDD12 Privacy", "```"+publicAccess+"```",true);
        panel.addField("\uD83C\uDFB5 Bitrate", "```"+bitrate+" kbps```",true);
        panel.addField("\uD83D\uDDFA️ Region", "```"+region.getName()+"```",true);
        panel.addField("\uD83D\uDC0C Slowmode", "```"+slowMode+"```",true);
        panel.addField("\uD83D\uDD1E NSFW", "```"+nsfw+"```",true);

        embeds.add(panel.build());
        return embeds;
    }

    public void updatePanel(Guild guild, VoiceChannel channel) {
        mongo.getParties().findByChannelID(channel.getIdLong(), party-> {
            if (party == null) {
                logger.error("Failed to update panel for party channel because party does not exist " + FoxFrame.getLogName(channel));
                return;
            }
            channel.retrieveMessageById(party.getPanelID()).queue(message -> {
                List<MessageEmbed> panel = getPanel(guild, channel, party);
                message.editMessageEmbeds(panel).queue();
                logger.info("Displayed data on the panel has been updated for party channel " + FoxFrame.getLogName(channel));
            }, error->{
                logger.error("Failed to update panel for party channel (PanelID: "+party.getPanelID()+") : " +error.getMessage() + " " + FoxFrame.getLogName(channel));
            });
        });
    }

    public void sendControlPanel(Guild guild, VoiceChannel channel, PartyEntry partyData) {
        List<MessageEmbed> panel = getPanel(guild,channel,partyData);

        Button channelRename = Button.primary("party_channel_rename", "\uD83D\uDCDB Rename");
        Button channelEdit = Button.primary("party_channel_edit", "✏️ Edit Channel");
        Button displayHelp = Button.primary("party_channel_help", "\uD83D\uDCDC Display Help");

        Button channelVisibility = Button.primary("party_channel_visibility", "\uD83E\uDD77 Change Visibility");
        Button channelPrivacy = Button.primary("party_channel_privacy", "\uD83D\uDD12 Change Privacy");
        Button channelNsfw = Button.primary("party_channel_nsfw", "\uD83D\uDD1E Change NSFW");

        List<EntitySelectMenu.DefaultValue> oldMembers = partyData.getMembers().stream()
                .filter(m->guild.getMemberById(m.getId()) != null)
                .map(m->EntitySelectMenu.DefaultValue.user(m.getId()))
                .toList();

        EntitySelectMenu memberMenu = EntitySelectMenu.create("party_member_list", EntitySelectMenu.SelectTarget.USER)
                .setDefaultValues(oldMembers)
                .build();


        channel.sendMessageEmbeds(panel)
                .addActionRow(
                        channelRename, channelEdit, displayHelp
                )
                .addActionRow(
                        channelVisibility, channelPrivacy, channelNsfw
                )
                .addActionRow(memberMenu)
                .queue(message -> {
                    mongo.getParties().findByChannelID(channel.getIdLong(), party-> {
                        if (party == null) {
                            logger.error("Failed to save panelID for party channel because party does not exist " + FoxFrame.getLogName(channel));
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

    public Modal renameModal(Member member, String currentName) { // UserLimit   bitrate region slowmode
        TextInput newNameField = TextInput.create("new_name", "New name", TextInputStyle.SHORT)
                .setPlaceholder(member.getUser().getGlobalName()+"'s Voice")
                .setValue(currentName)
                .setRequiredRange(1, 25)
                .build();

        return Modal.create("party_channel_rename_modal", "Rename Channel")
                .addComponents(ActionRow.of(newNameField))
                .build();
    }

    public Modal editModal(int userLimit, int bitRate, Region region, int slowMode) {
        TextInput userLimitField = TextInput.create("user_limit", "User limit", TextInputStyle.SHORT)
                .setPlaceholder("0-99")
                .setValue(String.valueOf(userLimit))
                .setRequiredRange(1, 2)
                .build();
        TextInput bitRateField = TextInput.create("bitrate", "Bitrate (kbps)", TextInputStyle.SHORT)
                .setPlaceholder("8-96")
                .setValue(String.valueOf(bitRate))
                .setRequiredRange(1, 3)
                .build();
        TextInput regionField = TextInput.create("region", "Region", TextInputStyle.SHORT)
                .setPlaceholder("automatic, brazil, hongkong, india, japan, rotterdam, singapore, south_africa, sydney, us-central...")
                .setValue(region.getKey())
                .setRequiredRange(4, 16)
                .build();
        TextInput slowModeField = TextInput.create("slowmode", "Slowmode", TextInputStyle.SHORT)
                .setPlaceholder(Arrays.stream(SlowMode.values()).map(SlowMode::getArg).collect(Collectors.joining(", ")))
                .setValue(SlowMode.fromValue(slowMode).getArg())
                .setRequiredRange(1, 3)
                .build();

        return Modal.create("party_channel_edit_modal", "Edit Channel")
                .addComponents(
                        ActionRow.of(userLimitField),
                        ActionRow.of(bitRateField),
                        ActionRow.of(regionField),
                        ActionRow.of(slowModeField)
                )
                .build();
    }

    public void hasPermissions(MessageChannelUnion channel, Member member, Runnable success, Consumer<String> error) {
        mongo.getParties().findByChannelID(channel.getIdLong(), (party)->{
            if (party == null) {
                logger.error("Failed to check permission on party channel because it does not exists or is invalid! " + FoxFrame.getLogName(channel));
                error.accept("Party channel does not exists or is invalid!");
                return;
            }
            if (member.getId().equals(party.getOwnerID())) {
                success.run();
                return;
            }
            for (PartyEntry.PartyMember partyMember : party.getMembers()) {
                if (partyMember.getId() == member.getIdLong() && partyMember.isAdmin()) {
                    success.run();
                }
            }
            error.accept("You do not have permissions for this action!");
        });

    }

    public String getNewPartyName(Member member) {
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

    public void setupPermissions(VoiceChannel channel, Member member, boolean isChannelAdmin, Runnable done) {
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

        channel.upsertPermissionOverride(member).setPermissions(allowed,denied).queue(s-> done.run());
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

    public void success(IReplyCallback event, String title, String message) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(FoxFrame.getSuccessColor());
        eb.setDescription("## ✅ "+title+"\n \n"+message+"\n\n*This message will be deleted in 30 seconds!*");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue(this::delayedDelete);
    }

    public void delayedDelete(InteractionHook message) {
        message.deleteOriginal().queueAfter(30, TimeUnit.SECONDS,
                success -> {}, // Do nothing on success
                failure -> {
                    if (failure instanceof ErrorResponseException error) {
                        if (error.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
                            return;
                        }
                    }
                    failure.printStackTrace();
                }
        );
    }

}
