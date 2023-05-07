package fi.natroutter.foxbot.interfaces;

import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.objects.BaseButton;
import fi.natroutter.foxbot.objects.BaseModal;
import fi.natroutter.foxbot.objects.BaseStringMenu;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class BaseCommand {

    private ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();

    public record removedCooldown(String userID, BaseCommand command){};

    @Getter
    private String name;

    @Getter @Setter
    private String description = "new command!";

    @Getter @Setter
    private boolean hidden = true;

    @Getter @Setter
    private List<BaseButton> buttons = new ArrayList<>();

    @Getter @Setter
    private List<BaseModal> modals = new ArrayList<>();

    @Getter @Setter
    private List<BaseStringMenu> stringMenus = new ArrayList<>();

    @Getter @Setter
    private boolean hideModalReply = true;

    @Getter @Setter
    private boolean commandReplyTypeModal = false;

    @Getter @Setter
    private int deleteDelay = 0;

    @Getter @Setter
    private int cooldownSeconds = 30; //30

    @Getter @Setter
    private int cooldownCleanInterval = 60; // 60

    @Getter @Setter
    private List<OptionData> arguments = new ArrayList<>();

    @Getter @Setter
    private Node permission;

    @Setter
    private Consumer<removedCooldown> onCooldownRemoved = data -> {
        System.out.println("Removed old cooldown from user \"" + data.userID() + "\" with \""+data.command().getCooldownSeconds()+" seconds\" in command \"" + data.command().getName() + "\"");
    };

    public BaseCommand(String name) {
        this.name = name;

        //Cleaning timer
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {clean();}
        }, 0, 1000L*cooldownCleanInterval);
    }

    public EmbedBuilder info(String msg) {
        return Utils.embedBase().setTitle("ℹ️ "+msg+"");
    }

    public EmbedBuilder error(String msg) {
        return Utils.embedBase().setTitle("❗ "+msg+"");

    }

    public OptionMapping getOption(List<OptionMapping> options, String name) {
        for (OptionMapping item : options) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    private void clean(){
        if (cooldowns.size() > 0) {
            cooldowns.forEach((id,cool)->{
                if (System.currentTimeMillis() > cool + (cooldownSeconds * 1000L)) {
                    cooldowns.remove(id);
                    onCooldownRemoved.accept(new removedCooldown(id, this));
                }
            });
        }
    }

    public long getCooldown(Member member) {
        if (cooldowns.containsKey(member.getId())) {
            long userCooldown = cooldowns.get(member.getId());
            return (userCooldown + (cooldownSeconds * 1000L)) / 1000 - System.currentTimeMillis() / 1000;
        }
        return 0;
    }

    public boolean isOnCooldown(Member member) {
        if (!cooldowns.containsKey(member.getId())) {
            return false;
        }
        long userCooldown = cooldowns.get(member.getId());

        if (System.currentTimeMillis() > userCooldown + (cooldownSeconds * 1000L)) {
            cooldowns.remove(member.getId());
            return false;
        }
        return true;
    }

    public void setCooldown(Member member) {
        cooldowns.put(member.getId(), System.currentTimeMillis());
    }

    public Button getButton(String id) {
        for (BaseButton button : buttons) {
            if (button.getId().equalsIgnoreCase(id)) {
                return button.getButton();
            }
        }
        return null;
    }

    public BaseModal getModal(String id) {
        for (BaseModal modal : modals) {
            if (modal.getId().equalsIgnoreCase(id)) {
                return modal;
            }
        }
        return null;
    }

    public BaseStringMenu getStringMenu(String id) {
        for (BaseStringMenu menu : stringMenus) {
            if (menu.getId().equalsIgnoreCase(id)) {
                return menu;
            }
        }
        return null;
    }

    public BaseCommand addModal(BaseModal... modal) {
        modals.addAll(List.of(modal));
        return this;
    }

    public BaseCommand addStringMenu(BaseStringMenu... menu) {
        stringMenus.addAll(List.of(menu));
        return this;
    }

    public BaseCommand addButton(BaseButton... button) {
        buttons.addAll(List.of(button));
        return this;
    }

    public BaseCommand addArguments(OptionData... data) {
        arguments.addAll(List.of(data));
        return this;
    }

    public ModalMapping getModalArg(List<ModalMapping> args, String id) {
        for (ModalMapping ent : args) {
            if (ent.getId().equalsIgnoreCase(id)) {
                return ent;
            }
        }
        return null;
    }

    public Object onButtonPress(Member member, User Bot, Guild guild, MessageChannel channel, BaseButton button) {
        return "This is default onButtonPress action!";
    };

    public Object onModalSubmit(Member member, User Bot, Guild guild, MessageChannel channel, BaseModal modal, List<ModalMapping> args) {
        return "This is default onModalSubmit reply!";
    };

    public Object onStringMenuSelect(Member member, User Bot, Guild guild, MessageChannel channel, BaseStringMenu menu, List<SelectOption> args) {
        return "This is default onStringMenuSelect reply!";
    };

    public abstract Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args);
}
