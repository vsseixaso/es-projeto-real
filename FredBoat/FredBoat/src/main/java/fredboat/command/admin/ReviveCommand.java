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

package fredboat.command.admin;

import fredboat.Config;
import fredboat.FredBoat;
import fredboat.command.util.HelpCommand;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.ICommandRestricted;
import fredboat.messaging.internal.Context;
import fredboat.perms.PermissionLevel;

import javax.annotation.Nonnull;

/**
 *
 * @author frederik
 */
public class ReviveCommand extends Command implements ICommandRestricted {

    public ReviveCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {

        int shardId;

        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        try {
            if (context.args.length > 1 && context.args[0].equals("guild")) {
                long guildId = Long.valueOf(context.args[1]);
                //https://discordapp.com/developers/docs/topics/gateway#sharding
                shardId = (int) ((guildId >> 22) % Config.CONFIG.getNumShards());
            } else {
                shardId = Integer.parseInt(context.args[0]);
            }
        } catch (NumberFormatException e) {
            HelpCommand.sendFormattedCommandHelp(context);
            return;
        }

        boolean force = context.rawArgs.toLowerCase().contains("force");

        context.replyWithName("Attempting to revive shard " + shardId);
        try {
            String answer = FredBoat.getShard(shardId).revive(force);
            context.replyWithName(answer);
        } catch (IndexOutOfBoundsException e) {
            context.replyWithName("No such shard: " + shardId);
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} <shardId> OR {0}{1} guild <guildId>\n#Revive the specified shard, or the shard of the specified guild.";
    }

    @Nonnull
    @Override
    public PermissionLevel getMinimumPerms() {
        return PermissionLevel.BOT_ADMIN;
    }
}
