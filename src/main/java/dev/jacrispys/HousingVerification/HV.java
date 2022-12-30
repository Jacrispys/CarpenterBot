package dev.jacrispys.HousingVerification;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.apache.ApacheHttpClient;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class HV extends ListenerAdapter {

    public static void main(String[] args) throws IOException {
        SecretData.initLoginInfo();
        JDA jda = JDABuilder.createDefault(SecretData.getToken())
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)
                .enableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE)
                .build();
        jda.getPresence().setPresence(Activity.competing(" Housing Awards"), false);
        connectionManager();
        thread.start();
        jda.addEventListener(getInstance());
        jda.updateCommands().addCommands(
                Commands.slash("verify", "Create Verify Message button")
        ).queue();


    }

    public HV() {
        INSTANCE = this;
    }

    private static HV INSTANCE;

    private static HV getInstance() {
        return INSTANCE != null ? INSTANCE : new HV();
    }

    private static Connection connection;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("verify")) {
            if (!event.getMember().getPermissions().contains(Permission.ADMINISTRATOR) && !event.getMember().getRoles().contains(event.getGuild().getRoleById(644645474149859361L))) {
                event.reply("Insufficient Permissions!").setEphemeral(true).queue();
            }
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.addContent("""
                    Click button below to verify your account!\s

                    **Steps to Verify**:
                    **1.** Click the `verify` button below.
                    **2.** Write down your current `IGN` into the prompt.
                    **3.** If it says your accounts are not linked, you may need to log into Hypixel and re-link your discord!

                    **NOTES**
                    Make sure you are logged into the correct minecraft account or the linking process may fail!
                    
                    *If you have already tried to verify, and updated your information, it may take up to 10 minutes to sync the new changes!*""");
            Button button = Button.success("verify", "Verify Account ✅");
            mcb.addActionRow(button);

            event.getChannel().sendMessage(mcb.build()).queue();
            event.reply("Success!").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify")) {
            TextInput input = TextInput.create("code", "Please enter your minecraft IGN:", TextInputStyle.SHORT).build();
            Modal modal = Modal.create("verify_modal", "Enter your IGN!").addActionRow(input).build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("verify_modal")) {
            try {
                Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                HypixelAPI api = new HypixelAPI(new ApacheHttpClient(UUID.fromString(SecretData.getHypixelToken())));
                String ign = event.getValue("code").getAsString();
                String tag;

                stmt.executeUpdate("DELETE FROM mc_auth WHERE date < (CURDATE() - INTERVAL 10 MINUTE )");

                ResultSet set = stmt.executeQuery("SELECT * FROM mc_auth WHERE ign='" + ign + "'");
                set.beforeFirst();
                if (!set.next()) {
                    tag = api.getPlayerByName(ign).get().getPlayer().getObjectProperty("socialMedia").getAsJsonObject("links").get("DISCORD").getAsString();
                    tag = tag.replace("!", "");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(new Date());
                    stmt.executeUpdate("REPLACE INTO mc_auth (ign, tag, date) VALUES ('" + ign + "', '" + tag + "', '" + currentTime + "');");
                } else {
                    tag = set.getString("tag");
                }
                if (tag.equals(event.getMember().getUser().getAsTag())) {
                    try {
                        event.getMember().modifyNickname(ign).queue();
                        if (event.getGuild().getIdLong() == 644644771176120358L) {
                            event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(1056740276993003540L)).queue();
                        }
                    } catch (HierarchyException ignored) {
                    }
                    event.reply("Successfully Verified account!").setEphemeral(true).queue();
                } else {
                    event.reply("Your IGN and linked discord account did not match! \n*If you recently updated your discord on Hypixel it may take up to 10 minutes to sync.*").setEphemeral(true).queue();
                    System.out.println(tag + "!=" + event.getMember().getUser().getAsTag());
                }
                stmt.close();
            } catch (SQLException | ExecutionException | InterruptedException ex) {
                event.reply("Cannot handle request right now! Please try again later.").setEphemeral(true).queue();
            }
        }
    }

    private static Connection resetConnection(String dataBase) throws SQLException {
        try {
            String userName = "Jacrispys";
            String db_password = SecretData.getDataBasePass();

            String url = "jdbc:mysql://" + SecretData.getDBHost() + ":3306/" + dataBase + "?autoReconnect=true";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            connection = DriverManager.getConnection(url, userName, db_password);
            return connection;


        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to the given database!");
        }
    }
    private static Thread thread;

    private static void connectionManager() {
        thread = new Thread(() -> {
            while (true) {
                try {
                    System.out.println("Enabling Database...");
                    connection = resetConnection("mc_discord");
                    System.out.println("Enabled!");
                    Thread.sleep(3600 * 1000);
                } catch (SQLException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
