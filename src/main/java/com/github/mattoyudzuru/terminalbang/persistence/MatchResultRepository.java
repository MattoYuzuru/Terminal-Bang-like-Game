package com.github.mattoyudzuru.terminalbang.persistence;

import com.github.mattoyudzuru.terminalbang.stats.LeaderboardEntry;
import com.github.mattoyudzuru.terminalbang.stats.MatchResult;

import java.util.List;

public interface MatchResultRepository {
    void save(MatchResult result);

    List<LeaderboardEntry> leaderboard(int limit);
}

