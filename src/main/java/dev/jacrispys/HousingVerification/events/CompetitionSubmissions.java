package dev.jacrispys.HousingVerification.events;

import dev.jacrispys.HousingVerification.mysql.SqlManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

public class CompetitionSubmissions extends ListenerAdapter {

    private static final Connection connection = SqlManager.getInstance().getSqlConnection();


    public CompetitionSubmissions() {

    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getGuild().getIdLong() == 644644771176120358L && event.getChannel().getIdLong() == 1038520377724379237L) {
            List<Role> roles = event.getMember().getRoles();
            for (Role role : roles) {
                if (role.getIdLong() == 644645474149859361L || role.getIdLong() == 1057812432862597131L) return;
            }
            TextChannel channel = event.getChannel().asTextChannel();
            try {
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("REPLACE INTO competition_submissions (message_id, user_id) VALUES ('" + event.getMessage().getIdLong() + "', '" + event.getMember().getIdLong() + "');");
                stmt.close();
                channel.getManager().putMemberPermissionOverride(event.getMember().getIdLong(), null, Collections.singletonList(Permission.MESSAGE_SEND)).queue();
            } catch (HierarchyException | SQLException ignored) {}
        }
    }


    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (event.getGuild().getIdLong() == 644644771176120358L && event.getChannel().getIdLong() == 1038520377724379237L) {
            TextChannel channel = event.getChannel().asTextChannel();
            try {
                Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet rs = stmt.executeQuery("SELECT * FROM competition_submissions WHERE message_id=" + event.getMessageIdLong());
                rs.beforeFirst();
                if (rs.next()) {
                    long memberId = rs.getLong("user_id");
                    Member member = event.getGuild().getMemberById(memberId);
                    List<Role> roles = member.getRoles();
                    for (Role role : roles) {
                        if (role.getIdLong() == 644645474149859361L || role.getIdLong() == 1057812432862597131L) return;
                    }
                    stmt.close();
                    rs.close();
                    channel.getManager().putMemberPermissionOverride(memberId, Collections.singletonList(Permission.MESSAGE_SEND), null).queue();
                }
            } catch (HierarchyException | SQLException ignored) {}
        }
    }
}
