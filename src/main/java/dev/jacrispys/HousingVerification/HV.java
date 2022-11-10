package dev.jacrispys.HousingVerification;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
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

import java.io.IOException;
import java.sql.*;

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
            if (!event.getMember().getPermissions().contains(Permission.ADMINISTRATOR)) {
                event.reply("Insufficient Permissions!").setEphemeral(true).queue();
            }
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.addContent("" +
                    "Click button below to verify your account! \n\n" +
                    "**Steps to Verify**:\n" +
                    "*1. Log into `verify.insideagent.pro` on your minecraft client.\n" +
                    "2. Write down the `code` given to you by the server.\n" +
                    "3. Come back to discord and click the `verify` button, and enter the code when prompted.*\n" +
                    "\n**NOTES**\n" +
                    "Make sure you are logged into the correct minecraft account or the linking process may fail!\n");
            Button button = Button.success("verify", "Verify Account ✅");
            mcb.addActionRow(button);

            event.getChannel().sendMessage(mcb.build()).queue();
            event.reply("Success!").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify")) {
            TextInput input = TextInput.create("code", "Submit Code (Format: XXXXX-XXXXX):", TextInputStyle.SHORT).build();
            Modal modal = Modal.create("verify_modal", "Enter your verification code!").addActionRow(input).build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("verify_modal")) {
            try {
                String code = event.getValue("code").getAsString();
                Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                ResultSet set = stmt.executeQuery("SELECT * FROM mc_auth WHERE `key`=CAST('" + code + "' AS BINARY);");
                set.beforeFirst();
                if (!set.next()) {
                    event.reply("Invalid auth code! Please try again.").setEphemeral(true).queue();
                    return;
                }
                String ign = set.getString("ign");
                try {
                    event.getMember().modifyNickname(ign).queue();
                } catch (HierarchyException ignored) {}
                stmt.executeUpdate("DELETE FROM mc_auth WHERE `key`=CAST('" + code + "' AS BINARY);");
                stmt.close();
                set.close();
                event.getGuild().addRoleToMember(event.getMember(), event.getGuild().getRoleById(1040086646252650517L)).queue();
                System.out.println("Added role to: " + ign);
                event.reply("Successfully Verified account!").setEphemeral(true).queue();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        if (event.getMember().getRoles().contains(event.getGuild().getRoleById(1040086646252650517L))) {
            event.getGuild().removeRoleFromMember(event.getMember(), event.getGuild().getRoleById(1040086646252650517L)).queue();
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
                    if (connection != null && !connection.isClosed()) {
                        Thread.sleep(3600 * 1000);
                        return;
                    }
                    System.out.println("Enabling Database...");
                    connection = resetConnection("mc_discord");
                    System.out.println("Enabled!");
                } catch (SQLException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
