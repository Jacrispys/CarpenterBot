package dev.jacrispys.HousingVerification.tickets;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TicketMsg extends ListenerAdapter {

    public TicketMsg() {

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ticketmsg")) {
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.setContent("""
                    To create a support ticket, please press the button below.\s
                    **WARNING:**\s
                    *Abuse of this system will result in no less than a 24hr timeout, and repeated abuse will result in a blacklisting!*""");
            Button button = Button.success("createTicket", "Create new ticket \uD83C\uDF9F️");
            mcb.addActionRow(button);
            event.getChannel().sendMessage(mcb.build()).queue();
            event.reply("Success!").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("createTicket")) {
            createTicket(event);
        } else {
            String buttonType = event.getComponentId().split(":")[0];
            String ticketId = event.getComponentId().split(":")[1];
            switch (buttonType) {
                case "closeTicket", "reviewed" -> closeTicket(event);
                case "help" -> helpButton(event);
                case "house" -> featuredHousing(event);
                case "house2" -> submitFeaturedHouse(event);
                case "application" -> System.out.println();
                case "back" -> goBackButton(event);
                case "report" -> reportUser(event);
                case "support" -> supportOption(event);
            }
        }
    }


    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().split(":")[0].equals("ticketReason")) {
            ThreadChannel thread = event.getGuild().getThreadChannelById((event.getModalId().split(":")[1]));
            event.reply("Ticket closed successfully.").setEphemeral(true).queue();
            thread.sendMessage("Thread closed by moderator. \n" +
                    "Reason: `" + event.getValue("reason").getAsString() + "`").queue();
            thread.getManager().setName("[CLOSED] " + thread.getName()).queue();
            thread.getManager().setLocked(true).queue();
            thread.getManager().setArchived(true).queue();
        } else if (event.getModalId().split(":")[0].equals("reportModal")) {
            ThreadChannel thread = event.getGuild().getThreadChannelById((event.getModalId().split(":")[1]));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor("Report Info", null, event.getMember().getUser().getEffectiveAvatarUrl());
            eb.setColor(0xa82314);
            eb.addField("Reported User: ", reportedUserMap.get(Long.parseLong(event.getModalId().split(":")[1])).getAsMention(), true);
            eb.addField("Report Message: ", event.getValue("reportInput:" + event.getModalId().split(":")[1]).getAsString(), true);
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.addEmbeds(eb.build());
            thread.sendMessage(mcb.build()).queue();
            event.reply("Your report has been recorded! A moderator will be with you shortly.").queue();
            String threadName = event.getChannel().getName();
            event.getChannel().asThreadChannel().getManager().setName("[OPEN] " + threadName).queue();
            event.getMessage().delete().queue();
            event.getChannel().asThreadChannel().getParentChannel().asTextChannel().getManager().putMemberPermissionOverride(event.getMember().getIdLong(), Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS), null).queue();
        } else if (event.getModalId().split(":")[0].equals("house2Modal")) {
            long channelId = Long.parseLong(event.getModalId().split(":")[1]);
            String ign = event.getValue("ignInput:" + channelId).getAsString();
            String houseName = event.getValue("houseNameInput:" + channelId).getAsString();
            String release = event.getValue("release:" + channelId) != null ? event.getValue("release:" + channelId).getAsString() : "";
            event.reply("Your response has been recorded!").setEphemeral(true).queue();
            event.getMessage().delete().queue();
            if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
                event.getChannel().asThreadChannel().removeThreadMember(event.getMember()).queueAfter(5, TimeUnit.SECONDS);
            }
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor("New Submission from " + event.getMember().getUser().getAsTag(), null, event.getMember().getUser().getEffectiveAvatarUrl());
            eb.setColor(0xe635ce);
            eb.addField("Submission Info: ", "- Submitted by: " + event.getMember().getAsMention() + "\n" +
                    "- Submitted at: <t:" + Instant.now().getEpochSecond() + ":f>", false);
            eb.addField("Owner's IGN: ", "`" + ign + "`", false);
            eb.addField("Housing name: ", "`" + houseName + "`", false);
            eb.addField("Estimated Release Date: ", "`" + (!release.equals("") ? release : "NOT RECORDED") + "`", false);
            mcb.setEmbeds(eb.build());
            event.getChannel().asThreadChannel().sendMessage(mcb.build()).queue();
            MessageCreateBuilder mcb2 = new MessageCreateBuilder();
            Button reviewed = Button.primary("reviewed:" + channelId, "Mark as reviewed");
            mcb2.setActionRow(reviewed);
            String threadName = event.getChannel().getName();
            event.getChannel().asThreadChannel().getManager().setName("[SUBMISSION] " + threadName).queue();
            event.getChannel().asThreadChannel().sendMessage(mcb2.build()).queue();

        } else if (event.getModalId().split(":")[0].equals("supportModal")) {
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor("Support Issue Description", null, event.getMember().getUser().getEffectiveAvatarUrl());
            eb.addField("", event.getValue("supportInput:" + event.getChannel().getIdLong()).getAsString(), false);
            eb.setColor(0x801123);
            mcb.setEmbeds(eb.build());
            event.reply("A moderator will be with you soon.").setEphemeral(true).queue();
            String threadName = event.getChannel().getName();
            event.getChannel().asThreadChannel().getManager().setName("[OPEN] " + threadName).queue();
            event.getMessage().delete().queue();
            event.getChannel().asThreadChannel().sendMessage(mcb.build()).queue();
            event.getChannel().asThreadChannel().getParentChannel().asTextChannel().getManager().putMemberPermissionOverride(event.getMember().getIdLong(), Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS), null).queue();
        }
    }

    private void supportOption(ButtonInteractionEvent event) {
        TextInput input = TextInput.create("supportInput:" + event.getChannel().getIdLong(), "Please describe your issue.", TextInputStyle.PARAGRAPH).setRequired(true).build();
        Modal modal = Modal.create("supportModal:" + event.getChannel().getIdLong(), "Issue Description").addActionRow(input).build();
        event.replyModal(modal).queue();
    }

    private void submitFeaturedHouse(ButtonInteractionEvent event) {
        TextInput ign = TextInput.create("ignInput:" + event.getChannel().getIdLong(), "House Owner's IGN", TextInputStyle.SHORT).setMaxLength(32).setRequired(true).build();
        TextInput houseName = TextInput.create("houseNameInput:" + event.getChannel().getIdLong(), "House's name", TextInputStyle.SHORT).setMaxLength(64).setRequired(true).build();
        TextInput releaseDate = TextInput.create("release:" + event.getChannel().getIdLong(), "Estimated Release date (Optional)", TextInputStyle.SHORT).setMaxLength(64).setRequired(false).build();
        Modal modal = Modal.create("house2Modal:" + event.getChannel().getIdLong(), "Featured Housing Details").addActionRows(ActionRow.of(ign), ActionRow.of(houseName), ActionRow.of(releaseDate)).build();
        event.replyModal(modal).queue();
    }

    private void featuredHousing(ButtonInteractionEvent event) {
        MessageEditBuilder meb = new MessageEditBuilder();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("Rules and Details for Submissions", null, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
        eb.addField("Please provide the following details", """
                1. IGN of the houses creator.\s

                2. Name of the housing (currently)\s
                                
                3. Estimated release of the house (optional)
                """, true);
        eb.addField("Rules for submission", "1. The house must remain public for visitation \n" +
                "2. Houses will only be reviewed once, duplicate submissions will be disregarded \n" +
                "3. Staff of the discord must be allowed to use screenshots of the house to highlight it", true);
        eb.setColor(0x7734eb);
        Button house2 = Button.success("house2:" + event.getChannel().getIdLong(), "Submit Now!");
        Button back = Button.secondary("back:" + event.getChannel().getIdLong(), "\uD83D\uDD19 Back");
        meb.setEmbeds(eb.build()).setActionRow(house2, back);
        event.editMessage(meb.build()).queue();
    }

    private void createTicket(ButtonInteractionEvent event) {
        UUID ticketUUID = UUID.randomUUID();
        TextChannel ticketChannel = event.getGuild().getTextChannelById(1061791370718744717L);
        TextChannel supportChannel = event.getGuild().getTextChannelById(1061868399241732176L);
        supportChannel.createThreadChannel("[PENDING] " + event.getUser().getName() + " - (" + ticketUUID + ")", true).setInvitable(false).queue(thread -> {
            ticketChannel.sendMessage("Created a ticket for " + event.getMember().getUser().getAsTag() + " at: <#" + thread.getIdLong() + ">").queue();
            supportChannel.getManager().putMemberPermissionOverride(event.getMember().getIdLong(), null, Collections.singleton(Permission.MESSAGE_SEND_IN_THREADS)).queue();
            thread.addThreadMember(event.getMember()).queue();
            event.reply("Created your ticket at: <#" + thread.getIdLong() + ">").setEphemeral(true).queue();
            thread.getManager().setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_24_HOURS).queue();
            MessageCreateBuilder mcb = new MessageCreateBuilder();
            mcb.setContent("Welcome to your ticket " + event.getMember().getAsMention() + "! Listed below are the details of the ticket, and a few questions to get you going before we send this to our support team.");
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(event.getMember().getUser().getAsTag() + "'s ticket");
            eb.addField("Time Created: ", "<t:" + Instant.now().getEpochSecond() + ":f>", true);
            eb.addField("Ticket Members: ", event.getMember().getAsMention(), true);
            eb.addField("Ticket Unique ID: ", "<#" + thread.getIdLong() + ">" + "\n`" + ticketUUID + "`", false);
            Random rand = new Random();
            int nextInt = rand.nextInt(0xffffff + 1);
            eb.setColor(nextInt);
            Button closeTicket = Button.danger("closeTicket:" + thread.getIdLong(), "Close Ticket.");
            mcb.addEmbeds(eb.build());
            mcb.addActionRow(closeTicket);
            thread.sendMessage(mcb.build()).queue();
            MessageCreateBuilder mcb2 = new MessageCreateBuilder();
            mcb2.setContent("\n" + event.getMember().getAsMention() + " Please select your ticket type below.");
            Button help = Button.danger("help:" + thread.getIdLong(), "Help / Player Report");
            Button house = Button.primary("house:" + thread.getIdLong(), "Featured Housing Suggestion");
            Button modApp = Button.secondary("application:" + thread.getIdLong(), "Moderator Applications").asDisabled();
            mcb2.addActionRow(help, house, modApp);
            thread.sendMessage(mcb2.build()).queue();

        });
    }

    private final Map<Long, IMentionable> reportedUserMap = new HashMap<>();

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        if (event.getComponentId().split(":")[0].equals("reportMenu")) {
            TextInput input = TextInput.create("reportInput:" + event.getChannel().getIdLong(), "Please enter a short reason for the report.", TextInputStyle.SHORT).setMaxLength(250).build();
            Modal modal = Modal.create("reportModal:" + event.getChannel().getIdLong(), "Report Input").addActionRow(input).build();
            event.replyModal(modal).queue();
            reportedUserMap.put(event.getChannel().getIdLong(), event.getValues().get(0));
        }
    }

    private void reportUser(ButtonInteractionEvent event) {
        SelectMenu menu = EntitySelectMenu.create("reportMenu:" + event.getChannel().getIdLong(), EntitySelectMenu.SelectTarget.USER).build();
        event.editMessage("Please select the user from the menu below.").setActionRow(menu).queue();
    }

    private void goBackButton(ButtonInteractionEvent event) {
        MessageEditBuilder meb = new MessageEditBuilder();
        meb.setContent("\n" + event.getMember().getAsMention() + " Please select your ticket type below.");
        Button help = Button.danger("help:" + event.getChannel().getIdLong(), "Help / Player Report");
        Button house = Button.primary("house:" + event.getChannel().getIdLong(), "Featured Housing Suggestion");
        Button modApp = Button.secondary("application:" + event.getChannel().getIdLong(), "Moderator Applications").asDisabled();
        meb.setActionRow(help, house, modApp);
        event.editMessage(meb.build()).setEmbeds().queue();
    }

    private void helpButton(ButtonInteractionEvent event) {
        MessageEditBuilder meb = new MessageEditBuilder();
        Button support = Button.success("support:" + event.getChannel().getIdLong(), "Support Ticket");
        Button report = Button.danger("report:" + event.getChannel().getIdLong(), "Report a User");
        Button back = Button.secondary("back:" + event.getChannel().getIdLong(), "\uD83D\uDD19 Back");
        meb.setActionRow(support, report, back);
        event.editMessage(meb.build()).queue();
    }

    private void closeTicket(ButtonInteractionEvent event) {
        ThreadChannel thread = event.getGuild().getThreadChannelById(event.getComponentId().split(":")[1]);
        for (Member members : thread.getMembers()) {
            if (!members.hasPermission(Permission.MANAGE_CHANNEL)) {
                thread.removeThreadMember(members).queue();
            }
        }
        if (event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            TextInput text = TextInput.create("reason", "Reason for closing thread.", TextInputStyle.SHORT).setMaxLength(250).build();
            Modal modal = Modal.create("ticketReason:" + thread.getIdLong(), "Please enter a reason for closing the thread.")
                    .addActionRow(text)
                    .build();
            event.replyModal(modal).queue();
        } else {
            event.reply("Closing thread.").setEphemeral(true).queue();
            thread.sendMessage("Thread closed by author.").queue();
            thread.getManager().setName("[ARCHIVED] " + thread.getName()).queue();
            thread.getManager().setLocked(true).queue();
            thread.getManager().setArchived(true).queue();
        }
    }

}
