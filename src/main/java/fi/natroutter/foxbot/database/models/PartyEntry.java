package fi.natroutter.foxbot.database.models;

import fi.natroutter.foxlib.mongo.MongoData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class PartyEntry implements MongoData {

    String ownerID;
    long channelID;
    String name;
    int userLimit;
    int bitRate;
    String region;
    int slowMode;
    boolean hidden;
    boolean publicAccess;
    boolean nswf;
    long panelID;
    List<PartyMember> members;

    public PartyEntry(String ownerID) {
        this.ownerID = ownerID;
        this.channelID = 0;
        this.name = "";
        this.userLimit = 0;
        this.bitRate = 64;
        this.region = "automatic";
        this.slowMode = 0;
        this.hidden = false;
        this.publicAccess = false;
        this.nswf = false;
        this.panelID = 0;
        this.members = new ArrayList<>();
    }

    @Override
    public String id() {
        return ownerID;
    }



    @AllArgsConstructor
    @NoArgsConstructor
    @Getter @Setter
    public static class PartyMember {
        long id = 0;
        boolean isAdmin = false;
    }
}
