package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxlib.mongo.ModelController;
import fi.natroutter.foxlib.mongo.MongoConnector;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class VoiceSessionController extends ModelController<VoiceSessionEntry> {

    private final AtomicBoolean indexesEnsured = new AtomicBoolean(false);

    public VoiceSessionController(MongoConnector connector) {
        super(connector, "voiceSessions", "sessionID", VoiceSessionEntry.class);
    }

    /**
     * Upserts every entry by {@code sessionID} in a single connection.
     *
     * <p>Plain inserts are wrong here: a periodic checkpoint may already have written a row for
     * the same session, and inserting again would duplicate it in the leaderboard.
     */
    public void saveAll(List<VoiceSessionEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        getCollection(sessions -> {
            ReplaceOptions options = new ReplaceOptions().upsert(true);
            for (VoiceSessionEntry entry : entries) {
                sessions.replaceOne(Filters.eq("sessionID", entry.getSessionID()), entry, options);
            }
        });
    }

    /**
     * Every record still flagged active, newest checkpoint first.
     *
     * <p>These are the sessions the previous run was in the middle of, whether it stopped cleanly
     * or not — the candidates for picking back up on startup.
     */
    public void findActive(Consumer<List<VoiceSessionEntry>> result) {
        getCollection(sessions -> result.accept(sessions.find(Filters.eq("active", true))
                .sort(Sorts.descending("endedAt"))
                .into(new ArrayList<>())));
    }

    /**
     * Clears the active flag on records left behind by the previous run, so interrupted sessions
     * are not shown as still running. Their stored duration stops at the last checkpoint.
     *
     * @param resumed sessions that have been picked back up and must stay active
     */
    public void closeOrphanedActive(Collection<String> resumed, Consumer<Long> closed) {
        getCollection(sessions -> {
            Bson filter = resumed.isEmpty()
                    ? Filters.eq("active", true)
                    : Filters.and(Filters.eq("active", true), Filters.nin("sessionID", resumed));

            long count = sessions.updateMany(filter, Updates.set("active", false)).getModifiedCount();
            closed.accept(count);
        });
    }

    public void getTopLongest(long guildID, int limit, Consumer<List<VoiceSessionEntry>> result) {
        getCollection(sessions -> {
            ensureIndexes(sessions);
            result.accept(sessions.find(Filters.eq("guildID", guildID))
                    .sort(Sorts.descending("durationSeconds"))
                    .limit(limit)
                    .into(new ArrayList<>()));
        });
    }

    public void getTopByChannel(long guildID, long channelID, int limit, Consumer<List<VoiceSessionEntry>> result) {
        getCollection(sessions -> {
            ensureIndexes(sessions);
            result.accept(sessions.find(Filters.and(
                            Filters.eq("guildID", guildID),
                            Filters.eq("channelID", channelID)
                    ))
                    .sort(Sorts.descending("durationSeconds"))
                    .limit(limit)
                    .into(new ArrayList<>()));
        });
    }

    public void getByUser(long guildID, String userID, int limit, Consumer<List<VoiceSessionEntry>> result) {
        getCollection(sessions -> {
            ensureIndexes(sessions);
            result.accept(sessions.find(Filters.and(
                            Filters.eq("guildID", guildID),
                            Filters.eq("participants.userID", userID)
                    ))
                    .sort(Sorts.descending("durationSeconds"))
                    .limit(limit)
                    .into(new ArrayList<>()));
        });
    }

    private void ensureIndexes(MongoCollection<VoiceSessionEntry> sessions) {
        if (!indexesEnsured.compareAndSet(false, true)) {
            return;
        }
        sessions.createIndex(Indexes.compoundIndex(
                Indexes.ascending("guildID"),
                Indexes.descending("durationSeconds")
        ));
        sessions.createIndex(Indexes.compoundIndex(
                Indexes.ascending("guildID"),
                Indexes.ascending("channelID"),
                Indexes.descending("durationSeconds")
        ));
    }
}
