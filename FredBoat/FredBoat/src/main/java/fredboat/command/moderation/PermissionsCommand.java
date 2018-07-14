/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.command.moderation;

import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IModerationCommand;
import fredboat.db.EntityReader;
import fredboat.db.EntityWriter;
import fredboat.db.entity.GuildPermissions;
import fredboat.feature.togglz.FeatureFlags;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;
import fredboat.perms.PermsUtil;
import fredboat.shared.constant.BotConstants;
import fredboat.util.ArgumentUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class PermissionsCommand extends Command implements IModerationCommand {

    private static final Logger log = LoggerFactory.getLogger(PermissionsCommand.class);

    public final PermissionLevel permissionLevel;

    public PermissionsCommand(PermissionLevel permissionLevel, String name, String... aliases) {
        super(name, aliases);
        this.permissionLevel = permissionLevel;
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        if (!FeatureFlags.PERMISSIONS.isActive()) {
            context.reply("Permissions are currently disabled.");
            return;
        }
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        switch (context.args[0]) {
            case "del":
            case "delete":
            case "remove":
            case "rem":
            case "rm":
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return;

                if (context.args.length < 2) {
                    HelpCommand.sendFormattedCommandHelp(context);
                    return;
                }

                remove(context);
                break;
            case "add":
                if (!PermsUtil.checkPermsWithFeedback(PermissionLevel.ADMIN, context)) return;

                if (context.args.length < 2) {
                    HelpCommand.sendFormattedCommandHelp(context);
                    return;
                }

                add(context);
                break;
            case "list":
            case "ls":
                list(context);
                break;
            default:
                HelpCommand.sendFormattedCommandHelp(context);
                break;
        }
    }

    public void remove(CommandContext context) {
        Guild guild = context.guild;
        Member invoker = context.invoker;
        //remove the first argument aka add / remove etc to get a nice search term
        String term = context.rawArgs.replaceFirst(context.args[0], "").trim();

        List<IMentionable> search = new ArrayList<>();
        search.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term));
        search.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false));
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        IMentionable selected = ArgumentUtil.checkSingleFuzzySearchResult(search, context, term);
        if (selected == null) return;

        if (!gp.getFromEnum(permissionLevel).contains(mentionableToId(selected))) {
            context.replyWithName(context. i18nFormat("permsNotAdded", "`" + mentionableToName(selected) + "`", "`" + permissionLevel + "`"));
            return;
        }

        List<String> newList = new ArrayList<>(gp.getFromEnum(permissionLevel));
        newList.remove(mentionableToId(selected));

        if (permissionLevel == PermissionLevel.ADMIN
                && PermissionLevel.BOT_ADMIN.getLevel() > PermsUtil.getPerms(invoker).getLevel()
                && !PermissionUtil.checkPermission(invoker, Permission.ADMINISTRATOR)
                && !PermsUtil.checkList(newList, invoker)) {
            context.replyWithName(context.i18n("permsFailSelfDemotion"));
            return;
        }

        gp.setFromEnum(permissionLevel, newList);
        EntityWriter.mergeGuildPermissions(gp);

        context.replyWithName(context.i18nFormat("permsRemoved", mentionableToName(selected), permissionLevel));
    }

    public void add(CommandContext context) {
        Guild guild = context.guild;
        //remove the first argument aka add / remove etc to get a nice search term
        String term = context.rawArgs.replaceFirst(context.args[0], "").trim();

        List<IMentionable> list = new ArrayList<>();
        list.addAll(ArgumentUtil.fuzzyRoleSearch(guild, term));
        list.addAll(ArgumentUtil.fuzzyMemberSearch(guild, term, false));
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        IMentionable selected = ArgumentUtil.checkSingleFuzzySearchResult(list, context, term);
        if (selected == null) return;

        if (gp.getFromEnum(permissionLevel).contains(mentionableToId(selected))) {
            context.replyWithName(context.i18nFormat("permsAlreadyAdded", "`" + mentionableToName(selected) + "`", "`" + permissionLevel + "`"));
            return;
        }

        List<String> newList = new ArrayList<>(gp.getFromEnum(permissionLevel));
        newList.add(mentionableToId(selected));
        gp.setFromEnum(permissionLevel, newList);
        EntityWriter.mergeGuildPermissions(gp);

        context.replyWithName(context.i18nFormat("permsAdded", mentionableToName(selected), permissionLevel));
    }

    public void list(CommandContext context) {
        Guild guild = context.guild;
        Member invoker = context.invoker;
        GuildPermissions gp = EntityReader.getGuildPermissions(guild);

        List<IMentionable> mentionables = idsToMentionables(guild, gp.getFromEnum(permissionLevel));

        String roleMentions = "";
        String memberMentions = "";

        for (IMentionable mentionable : mentionables) {
            if (mentionable instanceof Role) {
                if (((Role) mentionable).isPublicRole()) {
                    roleMentions = roleMentions + "@everyone" + "\n"; // Prevents ugly double double @@
                } else {
                    roleMentions = roleMentions + mentionable.getAsMention() + "\n";
                }
            } else {
                memberMentions = memberMentions + mentionable.getAsMention() + "\n";
            }
        }

        PermissionLevel invokerPerms = PermsUtil.getPerms(invoker);
        boolean invokerHas = PermsUtil.checkPerms(permissionLevel, invoker);

        if (roleMentions.isEmpty()) roleMentions = "<none>";
        if (memberMentions.isEmpty()) memberMentions = "<none>";

        EmbedBuilder eb = CentralMessaging.getColoredEmbedBuilder()
                .setTitle(context.i18nFormat("permsListTitle", permissionLevel))
                .setAuthor(invoker.getEffectiveName(), null, invoker.getUser().getAvatarUrl())
                .addField("Roles", roleMentions, true)
                .addField("Members", memberMentions, true)
                .addField(invoker.getEffectiveName(), (invokerHas ? ":white_check_mark:" : ":x:") + " (" + invokerPerms + ")", false);
        context.reply(CentralMessaging.addFooter(eb, guild.getSelfMember()).build());
    }

    private static String mentionableToId(IMentionable mentionable) {
        if (mentionable instanceof ISnowflake) {
            return ((ISnowflake) mentionable).getId();
        } else if (mentionable instanceof Member) {
            return ((Member) mentionable).getUser().getId();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String mentionableToName(IMentionable mentionable) {
        if (mentionable instanceof Role) {
            return ((Role) mentionable).getName();
        } else if (mentionable instanceof Member) {
            return ((Member) mentionable).getUser().getName();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static List<IMentionable> idsToMentionables(Guild guild, List<String> list) {
        List<IMentionable> out = new ArrayList<>();

        for (String id : list) {
            if (id.equals("")) continue;

            if (guild.getRoleById(id) != null) {
                out.add(guild.getRoleById(id));
                continue;
            }

            if (guild.getMemberById(id) != null) {
                out.add(guild.getMemberById(id));
            }
        }

        return out;
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        String usage = "{0}{1} add <role/user>\n{0}{1} del <role/user>\n{0}{1} list\n#";
        return usage + context.i18nFormat("helpPerms", permissionLevel.getName()) + "\n" + BotConstants.DOCS_PERMISSIONS_URL;
    }

}
