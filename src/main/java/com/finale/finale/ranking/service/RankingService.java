package com.finale.finale.ranking.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import com.finale.finale.ranking.dto.request.RankingResultRequest;
import com.finale.finale.ranking.dto.response.RankingResponse;
import com.finale.finale.ranking.dto.response.RankingResultResponse;
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
    private final UserRepository userRepository;

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
            String profileImage = "sf";
            if (userInfo != null) {
                String[] parts = userInfo.split(":");
                nickname = parts[0];
                profileImage = parts.length > 1 ? parts[1] : "sf";
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

    public RankingResultResponse processResult(Long userId, RankingResultRequest request) {
        LocalDate weekStart = getWeekStart();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Double currentScoreDouble = rankingRepository.getScore(weekStart, userId);
        int oldScore = currentScoreDouble != null ? currentScoreDouble.intValue() : 0;

        Integer startRank = rankingRepository.getMyRank(weekStart, userId);
        int totalParticipants = rankingRepository.getTotalParticipants(weekStart);

        if (startRank == null) {
            startRank = totalParticipants + 1;
            totalParticipants++;
        }

        int gainedScore = request.gainedScore();
        int newScore = oldScore + gainedScore;

        rankingRepository.addScore(
                weekStart,
                userId,
                gainedScore,
                user.getNickname(),
                user.getImageCategory().name()
        );

        Integer endRank = rankingRepository.getMyRank(weekStart, userId);
        if (endRank == null) {
            endRank = 1;
        }

        int rangeStart = Math.max(1, endRank - 3);
        int participantsAfterAdd = rankingRepository.getTotalParticipants(weekStart);
        int rangeEnd = Math.min(participantsAfterAdd, startRank + 3);

        List<RankingResultResponse.RankingResultEntry> rankingRange = getRankRange(weekStart, rangeStart, rangeEnd);

        return new RankingResultResponse(
                startRank,
                endRank,
                startRank - endRank,
                oldScore,
                newScore,
                rangeStart,
                rangeEnd,
                rankingRange
        );
    }

    private List<RankingResultResponse.RankingResultEntry> getRankRange(LocalDate weekStart, int rangeStart, int rangeEnd) {
        Collection<ScoredEntry<String>> entries = rankingRepository.getRankRangeEntries(
                weekStart,
                rangeStart - 1,
                rangeEnd - 1
        );

        if (entries.isEmpty()) {
            return List.of();
        }

        List<String> userIds = entries.stream()
                .map(ScoredEntry::getValue)
                .toList();

        Map<String, String> userInfos = rankingRepository.getUserInfos(weekStart, userIds);

        List<RankingResultResponse.RankingResultEntry> result = new ArrayList<>();
        int rank = rangeStart;

        for (ScoredEntry<String> entry : entries) {
            String odai = entry.getValue();
            String[] info = userInfos.getOrDefault(odai, "Unknown:sf").split(":");

            result.add(new RankingResultResponse.RankingResultEntry(
                    rank++,
                    Long.parseLong(odai),
                    info[0],
                    entry.getScore().intValue(),
                    info.length > 1 ? info[1] : "sf"
            ));
        }

        return result;
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
