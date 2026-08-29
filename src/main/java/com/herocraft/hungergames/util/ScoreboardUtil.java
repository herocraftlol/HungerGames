package com.herocraft.hungergames.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit et met à jour un tableau de score latéral individuel pour un joueur,
 * avec des équipes vides pour éviter les doublons de lignes identiques.
 */
public final class ScoreboardUtil {

    private ScoreboardUtil() {
    }

    public static void update(Player player, String title, List<String> lines) {
        ScoreboardManager manager = org.bukkit.Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
        }

        Objective objective = board.getObjective("hg_board");
        if (objective == null) {
            objective = board.registerNewObjective("hg_board", "dummy",
                    LegacyComponentSerializer.legacySection().deserialize(title));
        } else {
            objective.displayName(LegacyComponentSerializer.legacySection().deserialize(title));
        }
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

        // Nettoie les anciennes équipes/entrées.
        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }
        for (Team team : new ArrayList<>(board.getTeams())) {
            team.unregister();
        }

        int score = lines.size();
        int lineIndex = 0;
        for (String line : lines) {
            String entry = ChatColorCodes(lineIndex);
            Team team = board.registerNewTeam("hg_l" + lineIndex);
            team.addEntry(entry);
            team.prefix(LegacyComponentSerializer.legacySection().deserialize(line));
            objective.getScore(entry).setScore(score);
            score--;
            lineIndex++;
        }

        player.setScoreboard(board);
    }

    // Génère une entrée invisible unique par ligne (codes couleur invisibles empilés).
    private static String ChatColorCodes(int index) {
        StringBuilder sb = new StringBuilder();
        String hex = Integer.toHexString(index % 16);
        sb.append('§').append(hex).append('§').append('r');
        return sb.toString();
    }
}
