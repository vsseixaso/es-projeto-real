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

package fredboat.command.maintenance;

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import fredboat.Config;
import fredboat.FredBoat;
import fredboat.agent.FredBoatAgent;
import fredboat.audio.player.PlayerRegistry;
import fredboat.commandmeta.CommandManager;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IMaintenanceCommand;
import fredboat.feature.I18n;
import fredboat.messaging.CentralMessaging;
import fredboat.messaging.internal.Context;
import fredboat.util.AppInfo;
import fredboat.util.TextUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.entities.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;

public class StatsCommand extends Command implements IMaintenanceCommand {

    public StatsCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        context.reply(getStats(context, context.guild.getJDA()));
    }

    public static Message getStats(@Nullable Context context, @Nonnull JDA jda) {
        long totalSecs = (System.currentTimeMillis() - FredBoat.START_TIME) / 1000;
        int days = (int) (totalSecs / (60 * 60 * 24));
        int hours = (int) ((totalSecs / (60 * 60)) % 24);
        int mins = (int) ((totalSecs / 60) % 60);
        int secs = (int) (totalSecs % 60);

        //sorry for the ugly i18n handling but thats the price we pay for this to be accessible for bot admins through DMs
        final ResourceBundle i18n;
        if (context != null) {
            i18n = context.getI18n();
        } else {
            i18n = I18n.DEFAULT.getProps();
        }
        double commandsExecuted = CommandManager.totalCommandsExecuted.get();
        String str = MessageFormat.format(i18n.getString("statsParagraph"),
                days, hours, mins, secs, commandsExecuted - 1)
                + "\n";
        str = MessageFormat.format(i18n.getString("statsRate"), str,
                (float) (commandsExecuted - 1) / ((float) totalSecs / (float) (60 * 60)));

        str += "\n\n";
        String content = "";

        content += "Reserved memory:                " + Runtime.getRuntime().totalMemory() / 1000000 + "MB\n";
        content += "-> Of which is used:            " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000 + "MB\n";
        content += "-> Of which is free:            " + Runtime.getRuntime().freeMemory() / 1000000 + "MB\n";
        content += "Max reservable:                 " + Runtime.getRuntime().maxMemory() / 1000000 + "MB\n";

        content += "\n----------\n\n";

        content += "Sharding:                       " + jda.getShardInfo().getShardString() + "\n";
        content += "Players playing:                " + PlayerRegistry.getPlayingPlayers().size() + "\n";
        content += "Known servers:                  " + FredBoat.getTotalGuildsCount() + "\n";
        content += "Known users in servers:         " + FredBoat.getTotalUniqueUsersCount() + "\n";
        content += "Distribution:                   " + Config.CONFIG.getDistribution() + "\n";
        content += "JDA responses total:            " + jda.getResponseTotal() + "\n";
        content += "JDA version:                    " + JDAInfo.VERSION + "\n";
        content += "FredBoat version:               " + AppInfo.getAppInfo().getVersionBuild() + "\n";
        content += "Lavaplayer version:             " + PlayerLibrary.VERSION + "\n";

        content += "\n----------\n\n";

        content += "Last agent run times:\n";
        for (Map.Entry<Class<? extends FredBoatAgent>, Long> entry : FredBoatAgent.getLastRunTimes().entrySet()) {
            // [classname] [padded to length 32 with spaces] [formatted time]
            content += String.format("%1$-32s%2$s\n", entry.getKey().getSimpleName(),
                    TextUtils.asTimeInCentralEurope(entry.getValue()));
        }

        str += TextUtils.asCodeBlock(content);

        return CentralMessaging.getClearThreadLocalMessageBuilder().append(str).build();
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1}\n#Show some statistics about this bot.";
    }
}
