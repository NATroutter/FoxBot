package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.objects.BaseModal;
import fi.natroutter.foxbot.objects.ModalReply;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.List;

public class Vote extends BaseCommand {

    public Vote() {
        super("vote");
        this.setDescription("create a custom vote!");
        this.setHidden(false);
        this.setCommandReplyTypeModal(true);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        TextInput question = TextInput.create("subject", "Subject", TextInputStyle.SHORT)
                .setPlaceholder("Subject of this ticket")
                .setMinLength(10)
                .setMaxLength(100) // or setRequiredRange(10, 100)
                .build();

        TextInput body = TextInput.create("body", "Body", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Your concerns go here")
                .setMinLength(30)
                .setMaxLength(1000)
                .build();



        return new ModalReply("Create new vote",List.of(ActionRow.of(question), ActionRow.of(body)));
    }

    @Override
    public Object onModalSubmit(Member member, User Bot, Guild guild, MessageChannel channel, BaseModal modal, List<ModalMapping> args) {
        return "Vitun neekeri :D";
    }
}
