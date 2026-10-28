package com.deadseasons.command;

import com.deadseasons.Config;
import com.deadseasons.system.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * /deadseasons status | season <name> | bloodmoon | fog | airdrop | herd | raid | infect | cure | thirst <0-20>
 * Everything except "status" needs cheats/op.
 */
public final class DSCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("deadseasons")
                .then(Commands.literal("status").executes(DSCommand::status))
                .then(Commands.literal("season").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"summer", "autumn", "winter", "spring"}, b))
                                .executes(DSCommand::setSeason)))
                .then(Commands.literal("bloodmoon").requires(s -> s.hasPermission(2)).executes(DSCommand::bloodMoon))
                .then(Commands.literal("fog").requires(s -> s.hasPermission(2)).executes(DSCommand::fog))
                .then(Commands.literal("airdrop").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerLevel ow = c.getSource().getServer().overworld();
                    WorldEvents.airdrop(ow, ow.getRandom());
                    return 1;
                }))
                .then(Commands.literal("herd").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerLevel ow = c.getSource().getServer().overworld();
                    WorldEvents.herd(ow, ow.getRandom(), Seasons.dayIndex(ow));
                    return 1;
                }))
                .then(Commands.literal("raid").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerLevel ow = c.getSource().getServer().overworld();
                    WorldEvents.raid(ow, ow.getRandom(), Seasons.dayIndex(ow));
                    return 1;
                }))
                .then(Commands.literal("infect").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.getPersistentData().putLong("DSInfectedAt", p.serverLevel().getGameTime());
                    InfectionSystem.tick(p);
                    c.getSource().sendSuccess(() -> Component.literal("You are now infected."), false);
                    return 1;
                }))
                .then(Commands.literal("cure").requires(s -> s.hasPermission(2)).executes(c -> {
                    InfectionSystem.cure(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("thirst").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0, 20)).executes(c -> {
                            ThirstSystem.set(c.getSource().getPlayerOrException(), FloatArgumentType.getFloat(c, "value"));
                            return 1;
                        }))));
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        ServerLevel ow = c.getSource().getServer().overworld();
        long day = Seasons.dayIndex(ow);
        Seasons.Season s = Seasons.fromDay(day);
        ApocalypseData data = ApocalypseData.get(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.translatable("command.deadseasons.status",
                day + 1, Component.translatable(s.key()), Seasons.daysLeftInSeason(day),
                WorldEvents.daysUntilBloodMoon(day), Math.max(0, data.nextAirdropDay - day)), false);
        return 1;
    }

    private static int setSeason(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = StringArgumentType.getString(c, "name").toUpperCase();
        Seasons.Season parsed = null;
        for (Seasons.Season s : Seasons.Season.values()) if (s.name().equals(name)) parsed = s;
        if (parsed == null) {
            c.getSource().sendFailure(Component.literal("Unknown season: " + name.toLowerCase()));
            return 0;
        }
        final Seasons.Season target = parsed;
        ServerLevel ow = c.getSource().getServer().overworld();
        long day = Seasons.dayIndex(ow);
        long time = ow.getDayTime() % 24000L;
        int len = Config.SEASON_LENGTH_DAYS.get();
        long year = day / (len * 4L);
        long newDay = year * len * 4L + (long) target.ordinal() * len;
        if (newDay < day) newDay += len * 4L;
        ow.setDayTime(newDay * 24000L + time);
        c.getSource().sendSuccess(() -> Component.translatable("command.deadseasons.season_set", Component.translatable(target.key())), true);
        return 1;
    }

    private static int bloodMoon(CommandContext<CommandSourceStack> c) {
        ServerLevel ow = c.getSource().getServer().overworld();
        long day = Seasons.dayIndex(ow);
        int interval = Config.BLOOD_MOON_INTERVAL.get();
        long target = day;
        while (!WorldEvents.isBloodMoonDay(target)) target++;
        ow.setDayTime(target * 24000L + 12400L);
        c.getSource().sendSuccess(() -> Component.literal("Skipped to the next Blood Moon (every " + interval + " days)."), true);
        return 1;
    }

    private static int fog(CommandContext<CommandSourceStack> c) {
        ServerLevel ow = c.getSource().getServer().overworld();
        ApocalypseData data = ApocalypseData.get(c.getSource().getServer());
        long day = Seasons.dayIndex(ow);
        data.fogDay = day;
        data.fogRolledDay = day;
        data.setDirty();
        if (ow.getDayTime() % 24000L < 12500) ow.setDayTime(day * 24000L + 12600L);
        c.getSource().sendSuccess(() -> Component.literal("A fog night begins."), true);
        return 1;
    }

    private DSCommand() {}
}
