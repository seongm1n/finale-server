package com.finale.finale.ranking.dto.response;

import java.util.List;

public record RankingResultResponse(
        int startRank,
        int endRank,
        int rankUp,
        int oldScore,
        int newScore,
        int rangeStart,
        int rangeEnd,
        List<RankingResultEntry> rankingRange
) {
    public record RankingResultEntry(
            int rank,
            Long userId,
            String nickname,
            int score,
            String profileImage
    ) {}
}
