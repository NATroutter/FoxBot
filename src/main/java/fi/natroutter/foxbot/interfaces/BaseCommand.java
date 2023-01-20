package fi.natroutter.foxbot.interfaces;

import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.objects.BaseButton;
import fi.natroutter.foxbot.objects.BaseModal;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCommand {

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
    private boolean hideModalReply = true;

    @Getter @Setter
    private boolean commandReplyTypeModal = false;

    @Getter @Setter
    private Nodes permission;

    @Getter @Setter
    private int deleteDelay = 0;
    
    @Getter @Setter
    private List<OptionData> arguments = new ArrayList<>();

    public BaseCommand(String name) {
        this.name = name;
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

    public BaseCommand addModal(BaseModal... modal) {
        modals.addAll(List.of(modal));
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

    public Object onButtonPress(Member member, User Bot, Guild guild, MessageChannel channel, BaseButton button) {
        return "This is default onButtonPress action!";
    };

    public Object onModalSubmit(Member member, User Bot, Guild guild, MessageChannel channel, BaseModal modal, List<ModalMapping> args) {
        return "This is default onModalSubmit reply!";
    };

    public abstract Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args);
}
