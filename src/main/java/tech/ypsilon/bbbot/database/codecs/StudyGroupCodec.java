package tech.ypsilon.bbbot.database.codecs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.entities.User;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import tech.ypsilon.bbbot.database.MongoController;

import java.util.ArrayList;
import java.util.List;

public class StudyGroupCodec implements Codec<StudyGroupCodec> {

    public static final StudyGroupCodec EMPTY_CODEC = new StudyGroupCodec(null, null, null);

    private final ObjectId _id;
    private final String name;
    private final List<Long> users;

    public static StudyGroupCodec retrieveStudyGroup(String name) {
        return getCollection().find(Filters.eq("name", name)).first();
    }

    public static StudyGroupCodec retrieveStudyGroup(User user) {
        long userId = user.getIdLong();
        return getCollection().find(Filters.in("users", userId)).first();
    }

    public static StudyGroupCodec createGroup(String name, List<User> members) {
        StudyGroupCodec studyGroupCodec = retrieveStudyGroup(name);
        if(studyGroupCodec != null) {
            return null; //Group name already taken
        }
        if(members.size() == 0)
            throw new ArrayIndexOutOfBoundsException();
        List<Long> users = new ArrayList<>();
        members.forEach(user -> users.add(user.getIdLong()));
        studyGroupCodec = new StudyGroupCodec(new ObjectId(), name, users);
        getCollection().insertOne(studyGroupCodec);
        return studyGroupCodec;
    }

    public static boolean addToGroup(StudyGroupCodec group, User user) {
        StudyGroupCodec userGroup = retrieveStudyGroup(user);
        if(userGroup != null)
            return false;
        return getCollection().updateOne(Filters.eq("_id", group._id),
                Updates.addToSet("users", user.getIdLong())).wasAcknowledged();
    }

    private StudyGroupCodec(ObjectId _id, String name, List<Long> users) {
        this._id = _id;
        this.name = name;
        this.users = users;
    }

    public ObjectId getID() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public List<Long> getUserIDs() {
        return users;
    }

    @Override
    public StudyGroupCodec decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        ObjectId _id = reader.readObjectId("_id");
        String name = reader.readString("name");
        reader.readName("users");
        reader.readStartArray();
        List<Long> users = new ArrayList<>();
        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT)
            users.add(reader.readInt64());
        reader.readEndArray();
        reader.readEndDocument();
        return new StudyGroupCodec(_id, name, users);
    }

    @Override
    public void encode(BsonWriter writer, StudyGroupCodec data, EncoderContext encoderContext) {
        writer.writeStartDocument();

        writer.writeObjectId("_id", data._id);
        writer.writeString("name", data.name);
        writer.writeStartArray("users");
        for (Long userId : data.users)
            writer.writeInt64(userId);
        writer.writeEndArray();

        writer.writeEndDocument();
    }

    private static MongoCollection<StudyGroupCodec> getCollection() {
        return MongoController.getInstance().getCollection("studyGroups", StudyGroupCodec.class);
    }

    @Override
    public Class<StudyGroupCodec> getEncoderClass() {
        return StudyGroupCodec.class;
    }
}
