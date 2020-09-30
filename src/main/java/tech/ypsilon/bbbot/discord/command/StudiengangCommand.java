package tech.ypsilon.bbbot.discord.command;

import com.mongodb.client.MongoCollection;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.bson.Document;
import org.bson.types.ObjectId;
import tech.ypsilon.bbbot.database.MongoController;
import tech.ypsilon.bbbot.discord.DiscordController;
import tech.ypsilon.bbbot.util.EmbedUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StudiengangCommand extends Command {

    MongoCollection<Document> collection = MongoController.getInstance().getCollection("Studiengaenge");

    @Override
    public String[] getAlias() {
        return new String[]{"studiengang"};
    }

    @Override
    public String getDescription() {
        return "Füge einen neuen Studiengang hinzu";
    }

    private final String messageStart = "Herzlich willkommen auf dem Ersti-Server fürs <:KIT:759041596460236822> . " +
            "Wähle per Klick auf ein Emoji unter der Nachricht deinen Studiengang um die Informationen des Discord-Servers für dich zu personalisieren :star_struck: .\n";
    private final String messageEnd = "\n" + "Dein Studiengang fehlt? Schreibe einem Moderator <@&757718320526000138> :100:";

    @Override
    public void onExecute(GuildMessageReceivedEvent e, String[] args) {
        if(Objects.requireNonNull(e.getMember()).getRoles().stream().noneMatch(role -> role.getIdLong() == 759072770751201361L
                || role.getIdLong() == 757718320526000138L)) {
            e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Kein Recht",
                    "Du hast kein Recht diesen Befehl auszuführen", false).build()).queue();
            return;
        }

        switch (checkArgs(0, args, new String[]{"add", "remove", "list", "reload", "update"}, e)){
            case "add":
                if(args.length < 4){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Falsche Argumente",
                            "Eingabe: :emote @role Name", false).build()).queue();
                    return;
                }

                if(e.getMessage().getMentionedRoles().size() == 0){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Falsche Argumente",
                            "Es muss eine Rolle erwähnt werden", false).build()).queue();
                    return;
                }

                if(collection.countDocuments(new Document("roleId", e.getMessage().getMentionedRoles().get(0).getIdLong())) > 0){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Fehler beim Erstellen",
                            "Diese Rolle ist schon eingetragen", false).build()).queue();
                    return;
                }

                if(collection.countDocuments(new Document("emote", e.getMessage().getContentRaw().split(" ")[3])) > 0){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Fehler beim Erstellen",
                            "Der Emote wird schon benutzt", false).build()).queue();
                    return;
                }

                if(collection.countDocuments(new Document("name", args[3])) > 0){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Fehler beim Erstellen",
                            "Der Name wird schon benutzt", false).build()).queue();
                    return;
                }

                collection.insertOne(new Document("_id", new ObjectId())
                        .append("roleId", e.getMessage().getMentionedRoles().get(0).getIdLong())
                        .append("emote", e.getMessage().getContentRaw().split(" ")[3])
                        .append("name", args[3]));

                e.getChannel().sendMessage(EmbedUtil.createSuccessEmbed()
                        .addField("Studiengang hinzugefügt", "Der Studiengang wurde erfolgreich hinzugefügt",
                                false).build()).queue();
                break;
            case "remove":
                if(args.length < 2){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Falsche Argumente",
                            "Eingabe: @role", false).build()).queue();
                    return;
                }


                if(e.getMessage().getMentionedRoles().size() == 0){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Falsche Argumente",
                            "Es muss eine Rolle erwähnt werden", false).build()).queue();

                }

                if(collection.countDocuments(new Document("roleId", e.getMessage().getMentionedRoles().get(0).getIdLong())) == 0){
                    e.getChannel().sendMessage(EmbedUtil.createErrorEmbed().addField("Fehler beim Löschen",
                            "Diese Rolle ist noch nicht eingetragen", false).build()).queue();
                    return;
                }

                collection.deleteOne(new Document("roleId", e.getMessage().getMentionedRoles().get(0).getIdLong()));
                e.getChannel().sendMessage(EmbedUtil.createSuccessEmbed()
                        .addField("Studiengang entfernt", "Der Studiengang wurde erfolgreich entfernt",
                                false).build()).queue();
            case "list":
                StringBuilder list = new StringBuilder();
                for(Document doc : collection.find()){
                    list.append(", ").append(doc.getString("name"));
                }
                list = new StringBuilder(list.toString().replace(", ", ""));

                e.getChannel().sendMessage(EmbedUtil.createInfoEmbed().addField("Studiengänge", list.toString(), false).build()).queue();
            case "update":
                List<String> emotes = new ArrayList<>();

                StringBuilder msg = new StringBuilder();
                for(Document doc : collection.find()){
                    emotes.add(doc.getString("emote"));
                    msg.append(doc.getString("emote")).append(" - ").append(doc.getString("name")).append("\n");
                }

                TextChannel textChannel = Objects.requireNonNull(DiscordController.getJDA().getTextChannelById("759033520680599553"));
                textChannel
                        .retrieveMessageById("759043590432882798").queue(message -> {
                            message.editMessage(messageStart + msg.toString() + messageEnd).queue();

                            for(String emote : emotes){
                                textChannel.retrievePinnedMessages().queue(messages -> {
                                    ReactionTemp reactionTemp = new ReactionTemp(textChannel, messages);

                                    if(!reactionTemp.contains(emote)) {
                                        reactionTemp.addEmote(emote);
                                    }
                                });

                                //for(MessageReaction reaction : message.getReactions()){
                                //    if(!reaction.getReactionEmote().getEmoji().equals(emote)){
                                //        message.addReaction(emote).queue();
                                //    }
                                //}
                            }
                });

                e.getChannel().sendMessage(EmbedUtil.createSuccessEmbed()
                        .addField("Nachricht wird aktualisiert", "Die Nachricht wird jetzt aktualisiert",
                                false).build()).queue();
        }
    }

    private class ReactionTemp {

        private TextChannel channel;
        private List<Message> messages;
        private List<MessageReaction> reactions = new ArrayList<>();

        public ReactionTemp(TextChannel channel, List<Message> msg) {
            this.messages = msg;
            this.channel = channel;
            msg.forEach(m -> reactions.addAll(m.getReactions()));
        }

        public List<MessageReaction> getReactions() {
            return reactions;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public void addEmote(String emoji) {
            boolean finished = false;
            for (Message reactionTempMessage : messages) {
                if(reactionTempMessage.getReactions().size() < 19) {
                    reactionTempMessage.addReaction(emoji).queue();
                    finished = true;
                    break;
                }
            }
            if(!finished) {
                Message complete = channel.sendMessage("-").complete();
                complete.addReaction(emoji).queue();
            }
        }

        public boolean contains(String emote) {
            for (MessageReaction reaction : reactions) {
                if(reaction.getReactionEmote().getEmoji().equals(emote))
                    return true;
            }
            return false;
        }

    }

}
