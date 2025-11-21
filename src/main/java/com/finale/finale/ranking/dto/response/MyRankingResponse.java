package com.finale.finale.ranking.dto.response;

import java.time.LocalDate;

public record MyRankingResponse(
        Integer myRanking,
        Integer score,
        Integer totalParticipants,
        String seasonName,
        LocalDate startDate,
        LocalDate endDate,
        TimeLeft timeLeft
) {
}
