package dev.jacrispys.HousingVerification;

import dev.jacrispys.HousingVerification.commands.VerifyCommand;
import dev.jacrispys.HousingVerification.events.CompetitionSubmissions;
import dev.jacrispys.HousingVerification.mysql.SqlManager;
import dev.jacrispys.HousingVerification.tickets.TicketMsg;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
        SqlManager.getInstance();
        jda.getPresence().setPresence(Activity.competing(" Housing Awards"), false);
        jda.addEventListener(new CompetitionSubmissions());
        jda.addEventListener(new TicketMsg());
        jda.addEventListener(new VerifyCommand());
        jda.updateCommands().addCommands(
                Commands.slash("verify", "Create Verify Message button"),
                Commands.slash("ticketmsg", "Create Ticket Message Button")
        ).queue();


    }

    public HV() {
    }

    private static HV INSTANCE;

    private static HV getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new HV();
        }

        return INSTANCE;
    }

}
