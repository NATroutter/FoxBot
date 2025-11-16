package fi.natroutter.foxbot.database.controllers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.PartyEntry;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.mongo.ModelController;
import fi.natroutter.foxlib.mongo.MongoConnector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class PartyController extends ModelController<PartyEntry> {

    public PartyController(MongoConnector connector) {
        super(connector, "parties", "ownerID", PartyEntry.class);
    }

    @Override
    public void findByID(String ownerID, @NotNull Consumer<PartyEntry> entry) {
        super.findByID(ownerID, data-> {
            if (data == null) {
                data = new PartyEntry(ownerID);
                save(data);
            }
            entry.accept(data);
        });
    }

    public void findByChannelID(long ChannelID, @Nullable Consumer<PartyEntry> entry) {
        super.findBy("channelID", ChannelID, entry);
    }
}
