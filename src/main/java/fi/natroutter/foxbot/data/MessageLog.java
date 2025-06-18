package fi.natroutter.foxbot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

@Getter
@Setter
@AllArgsConstructor
public class MessageLog {
    private Message message;
    private Message edited;
    private Guild guild;
    private MessageChannelUnion channel;
}
