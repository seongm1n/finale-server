package com.finale.finale.ranking.repository;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RankingRepository {

    private final RedissonClient redissonClient;

    public void addScore(LocalDate weekStart, Long userId, int score, String nickname, String profileImage) {
        String scoreKey = getScoreKey(weekStart);
        String userKey = getUserKey(weekStart);

        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(scoreKey);
        scoredSet.addScore(userId.toString(), score);
        scoredSet.expire(Duration.ofDays(14));

        RMap<String, String> userMap = redissonClient.getMap(userKey);
        userMap.put(userId.toString(), nickname + ":" + profileImage);
        userMap.expire(Duration.ofDays(14));
    }

    public Integer getMyRank(LocalDate weekStart, Long userId) {
        String scoreKey = getScoreKey(weekStart);
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(scoreKey);

        Integer rank = scoredSet.revRank(userId.toString());
        return rank != null ? rank + 1 : null;
    }

    public int getTotalParticipants(LocalDate weekStart) {
        String scoreKey = getScoreKey(weekStart);
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(scoreKey);
        return scoredSet.size();
    }

    public Collection<ScoredEntry<String>> getTopRankings(LocalDate weekStart) {
        String scoreKey = getScoreKey(weekStart);
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(scoreKey);
        return scoredSet.entryRangeReversed(0, -1);
    }

    public Map<String, String> getUserInfos(LocalDate weekStart, Collection<String> userIds) {
        String userKey = getUserKey(weekStart);
        RMap<String, String> userMap = redissonClient.getMap(userKey);
        return userMap.getAll(new HashSet<>(userIds));
    }

    public void updateUserInfo(LocalDate weekStart, Long userId, String nickname, String profileImage) {
        String userKey = getUserKey(weekStart);
        RMap<String, String> userMap = redissonClient.getMap(userKey);

        if (userMap.containsKey(userId.toString())) {
            userMap.put(userId.toString(), nickname + ":" + profileImage);
        }
    }

    public Double getScore(LocalDate weekStart, Long userId) {
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(getScoreKey(weekStart));
        return scoredSet.getScore(userId.toString());
    }

    public Collection<ScoredEntry<String>> getRankRangeEntries(LocalDate weekStart, int startIndex, int endIndex) {
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(getScoreKey(weekStart));
        return scoredSet.entryRangeReversed(startIndex, endIndex);
    }

    private String getScoreKey(LocalDate weekStart) {
        return "ranking:score:" + weekStart;
    }

    private String getUserKey(LocalDate weekStart) {
        return "ranking:user:" + weekStart;
    }
}
