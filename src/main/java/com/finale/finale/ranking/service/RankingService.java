package com.finale.finale.ranking.service;

import com.finale.finale.ranking.dto.RankingResponse;
import com.finale.finale.ranking.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository rankingRepository;

    public RankingResponse getRankings(Long userId) {
        LocalDate weekStart = getWeekStart();
        LocalDate weekEnd = weekStart.plusDays(6);

        Integer myRanking = rankingRepository.getMyRank(weekStart, userId);
        int totalParticipants = rankingRepository.getTotalParticipants(weekStart);

        Collection<ScoredEntry<String>> topScores = rankingRepository.getTopRankings(weekStart);

        List<String> userIds = topScores.stream()
                .map(ScoredEntry::getValue)
                .toList();

        Map<String, String> userInfos = userIds.isEmpty()
                ? Map.of()
                : rankingRepository.getUserInfos(weekStart, userIds);

        List<RankingResponse.RankingEntry> rankings = new ArrayList<>();
        int rank = 1;
        for (ScoredEntry<String> entry : topScores) {
            String userIdStr = entry.getValue();
            String userInfo = userInfos.get(userIdStr);

            String nickname = "Unknown";
            String profileImage = "DEFAULT";
            if (userInfo != null) {
                String[] parts = userInfo.split(":");
                nickname = parts[0];
                profileImage = parts.length > 1 ? parts[1] : "DEFAULT";
            }

            rankings.add(new RankingResponse.RankingEntry(
                    rank++,
                    Long.parseLong(userIdStr),
                    nickname,
                    entry.getScore().intValue(),
                    profileImage
            ));
        }

        return new RankingResponse(
                getSeasonName(weekStart),
                weekStart,
                weekEnd,
                calculateTimeLeft(weekEnd),
                myRanking,
                totalParticipants,
                rankings
        );
    }

    public void updateUserInfo(Long userId, String nickname, String profileImage) {
        LocalDate weekStart = getWeekStart();
        rankingRepository.updateUserInfo(weekStart, userId, nickname, profileImage);
    }

    private LocalDate getWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private String getSeasonName(LocalDate weekStart) {
        int weekOfMonth = (weekStart.getDayOfMonth() - 1) / 7 + 1;
        return weekStart.getMonthValue() + "월 " + weekOfMonth + "주차";
    }

    private RankingResponse.TimeLeft calculateTimeLeft(LocalDate weekEnd) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfSeason = weekEnd.atTime(23, 59, 59);

        Duration duration = Duration.between(now, endOfSeason);
        if (duration.isNegative()) {
            return new RankingResponse.TimeLeft(0, 0, 0);
        }

        int days = (int) duration.toDays();
        int hours = (int) (duration.toHours() % 24);
        int minutes = (int) (duration.toMinutes() % 60);

        return new RankingResponse.TimeLeft(days, hours, minutes);
    }
}
