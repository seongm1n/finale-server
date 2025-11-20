package com.finale.finale.ranking.dto.response;

import java.time.LocalDate;
import java.util.List;

public record RankingResponse(
        String seasonName,
        LocalDate startDate,
        LocalDate endDate,
        TimeLeft timeLeft,
        Integer myRanking,
        Integer totalParticipants,
        List<RankingEntry> rankings
) {
    public record TimeLeft(
            int days,
            int hours,
            int minutes
    ) {}

    public record RankingEntry(
            int rank,
            Long userId,
            String nickname,
            int score,
            String profileImage
    ) {}
}
