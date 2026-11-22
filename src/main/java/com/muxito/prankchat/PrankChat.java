package com.muxito.prankchat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrankChat extends JavaPlugin implements Listener {

    private static final String[] BADGES = {
            "\u2620\u2460", "\u2620\u2461", "\u2620\u2462", "\u2620\u2463", "\u2620\u2464"
    };
    private static final NamedTextColor[] LEVEL_COLORS = {
            NamedTextColor.GREEN, NamedTextColor.YELLOW, NamedTextColor.GOLD,
            NamedTextColor.RED, NamedTextColor.DARK_RED
    };

    private record Info(String rank, int level) { }

    private final Map<UUID, Info> cache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::refreshAll, 1L, 10L);
    }

    private void refreshAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            refresh(p);
        }
    }

    private void refresh(Player p) {
        Set<String> tags = p.getScoreboardTags();
        String rank = "MEMBER";
        if (tags.contains("rank_admin")) {
            rank = "ADMIN";
        } else if (tags.contains("rank_mod")) {
            rank = "MOD";
        }
        int level = 0;
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = board.getObjective("prank");
        if (objective != null) {
            level = objective.getScore(p.getName()).getScore();
        }
        cache.put(p.getUniqueId(), new Info(rank, level));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        event.renderer(ChatRenderer.viewerUnaware(
                (source, sourceDisplayName, message) -> format(source, message)));
    }

    private Component format(Player source, Component message) {
        Info info = cache.getOrDefault(source.getUniqueId(), new Info("MEMBER", 0));

        NamedTextColor rankColor = switch (info.rank()) {
            case "ADMIN" -> NamedTextColor.RED;
            case "MOD" -> NamedTextColor.GREEN;
            default -> NamedTextColor.BLUE;
        };

        Component out = Component.empty();
        out = out.append(Component.text("[" + info.rank() + "] ", rankColor, TextDecoration.BOLD));
        out = out.append(Component.object(
                ObjectContents.playerHead().name(source.getName()).hat(true).build()));
        out = out.append(Component.text(" "));
        out = out.append(Component.text(source.getName(), NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, false));

        int level = info.level();
        if (level >= 1 && level <= 5) {
            String body = getConfig().getString("levels." + level, "");
            Component tooltip = Component.text("LEVEL " + level + ":\n" + body, NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, false);
            out = out.append(Component.text(" " + BADGES[level - 1], LEVEL_COLORS[level - 1])
                    .decoration(TextDecoration.BOLD, false)
                    .hoverEvent(HoverEvent.showText(tooltip)));
        }

        out = out.append(Component.text(" \u00bb ", NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, false));
        out = out.append(message);
        return out;
    }
}
