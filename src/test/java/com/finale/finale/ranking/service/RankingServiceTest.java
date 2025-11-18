package com.finale.finale.ranking.service;

import com.finale.finale.auth.domain.User;
import com.finale.finale.auth.domain.UserImageCategory;
import com.finale.finale.auth.repository.UserRepository;
import com.finale.finale.exception.CustomException;
import com.finale.finale.ranking.dto.request.RankingResultRequest;
import com.finale.finale.ranking.dto.response.RankingResponse;
import com.finale.finale.ranking.dto.response.RankingResultResponse;
import com.finale.finale.ranking.repository.RankingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.client.protocol.ScoredEntry;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankingService 테스트")
class RankingServiceTest {

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RankingService rankingService;

    @Test
    @DisplayName("랭킹 조회 성공 - 전체 랭킹 정보 반환")
    void getRankingsSuccess() {
        // Given
        Long userId = 1L;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        given(rankingRepository.getMyRank(weekStart, userId)).willReturn(5);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(100);

        ScoredEntry<String> entry1 = createScoredEntry("10", 9820.0);
        ScoredEntry<String> entry2 = createScoredEntry("20", 9340.0);
        ScoredEntry<String> entry3 = createScoredEntry("30", 8900.0);
        Collection<ScoredEntry<String>> topScores = List.of(entry1, entry2, entry3);

        given(rankingRepository.getTopRankings(weekStart)).willReturn(topScores);
        given(rankingRepository.getUserInfos(eq(weekStart), any()))
                .willReturn(Map.of(
                        "10", "user1:CAT",
                        "20", "user2:DOG",
                        "30", "user3:RABBIT"
                ));

        // When
        RankingResponse response = rankingService.getRankings(userId);

        // Then
        assertThat(response.myRanking()).isEqualTo(5);
        assertThat(response.totalParticipants()).isEqualTo(100);
        assertThat(response.rankings()).hasSize(3);
        assertThat(response.rankings().get(0).rank()).isEqualTo(1);
        assertThat(response.rankings().get(0).userId()).isEqualTo(10L);
        assertThat(response.rankings().get(0).nickname()).isEqualTo("user1");
        assertThat(response.rankings().get(0).score()).isEqualTo(9820);
        assertThat(response.rankings().get(0).profileImage()).isEqualTo("CAT");
    }

    @Test
    @DisplayName("랭킹 조회 성공 - 참여하지 않은 사용자 (myRanking null)")
    void getRankingsNotParticipated() {
        // Given
        Long userId = 999L;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        given(rankingRepository.getMyRank(weekStart, userId)).willReturn(null);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(50);
        given(rankingRepository.getTopRankings(weekStart)).willReturn(List.of());

        // When
        RankingResponse response = rankingService.getRankings(userId);

        // Then
        assertThat(response.myRanking()).isNull();
        assertThat(response.totalParticipants()).isEqualTo(50);
        assertThat(response.rankings()).isEmpty();
    }

    @Test
    @DisplayName("랭킹 조회 성공 - 빈 랭킹 (참여자 없음)")
    void getRankingsEmpty() {
        // Given
        Long userId = 1L;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        given(rankingRepository.getMyRank(weekStart, userId)).willReturn(null);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(0);
        given(rankingRepository.getTopRankings(weekStart)).willReturn(List.of());

        // When
        RankingResponse response = rankingService.getRankings(userId);

        // Then
        assertThat(response.totalParticipants()).isEqualTo(0);
        assertThat(response.rankings()).isEmpty();
    }

    @Test
    @DisplayName("랭킹 조회 성공 - 시즌 정보 확인")
    void getRankingsSeasonInfo() {
        // Given
        Long userId = 1L;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        given(rankingRepository.getMyRank(weekStart, userId)).willReturn(1);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(10);
        given(rankingRepository.getTopRankings(weekStart)).willReturn(List.of());

        // When
        RankingResponse response = rankingService.getRankings(userId);

        // Then
        assertThat(response.startDate()).isEqualTo(weekStart);
        assertThat(response.endDate()).isEqualTo(weekEnd);
        assertThat(response.seasonName()).contains("월");
        assertThat(response.seasonName()).contains("주차");
        assertThat(response.timeLeft()).isNotNull();
    }

    @Test
    @DisplayName("랭킹 조회 성공 - 사용자 정보 없는 경우 기본값 사용")
    void getRankingsWithMissingUserInfo() {
        // Given
        Long userId = 1L;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        given(rankingRepository.getMyRank(weekStart, userId)).willReturn(1);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(1);

        ScoredEntry<String> entry = createScoredEntry("10", 1000.0);
        given(rankingRepository.getTopRankings(weekStart)).willReturn(List.of(entry));
        given(rankingRepository.getUserInfos(eq(weekStart), any())).willReturn(Map.of());

        // When
        RankingResponse response = rankingService.getRankings(userId);

        // Then
        assertThat(response.rankings().get(0).nickname()).isEqualTo("Unknown");
        assertThat(response.rankings().get(0).profileImage()).isEqualTo("sf");
    }

    @Test
    @DisplayName("사용자 정보 업데이트 성공")
    void updateUserInfoSuccess() {
        // Given
        Long userId = 1L;
        String nickname = "newNickname";
        String profileImage = "CAT";
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        // When
        rankingService.updateUserInfo(userId, nickname, profileImage);

        // Then
        verify(rankingRepository).updateUserInfo(weekStart, userId, nickname, profileImage);
    }

    @Test
    @DisplayName("랭킹 조회 성공 - 프로필 이미지 없는 경우 기본값")
    void getRankingsWithoutProfileImage() {
        // Given
        Long userId = 1L;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        given(rankingRepository.getMyRank(weekStart, userId)).willReturn(1);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(1);

        ScoredEntry<String> entry = createScoredEntry("10", 1000.0);
        given(rankingRepository.getTopRankings(weekStart)).willReturn(List.of(entry));
        given(rankingRepository.getUserInfos(eq(weekStart), any()))
                .willReturn(Map.of("10", "user1"));

        // When
        RankingResponse response = rankingService.getRankings(userId);

        // Then
        assertThat(response.rankings().get(0).nickname()).isEqualTo("user1");
        assertThat(response.rankings().get(0).profileImage()).isEqualTo("sf");
    }

    private ScoredEntry<String> createScoredEntry(String value, Double score) {
        return new ScoredEntry<>(score, value);
    }

    @Test
    @DisplayName("랭킹 결과 처리 성공 - 기존 사용자 랭킹 상승")
    void processResultSuccess() {
        // Given
        Long userId = 1L;
        int gainedScore = 120;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        User user = createUser(userId, "testUser", UserImageCategory.sf);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        given(rankingRepository.getScore(weekStart, userId)).willReturn(4090.0);
        given(rankingRepository.getMyRank(weekStart, userId))
                .willReturn(152)  // before
                .willReturn(138); // after
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(200);

        ScoredEntry<String> entry1 = createScoredEntry("20", 4500.0);
        ScoredEntry<String> entry2 = createScoredEntry("1", 4210.0);
        ScoredEntry<String> entry3 = createScoredEntry("30", 4000.0);
        given(rankingRepository.getRankRangeEntries(eq(weekStart), any(Integer.class), any(Integer.class)))
                .willReturn(List.of(entry1, entry2, entry3));
        given(rankingRepository.getUserInfos(eq(weekStart), any()))
                .willReturn(Map.of(
                        "20", "alpha:CAT",
                        "1", "testUser:sf",
                        "30", "beta:DOG"
                ));

        // When
        RankingResultResponse response = rankingService.processResult(userId, new RankingResultRequest(gainedScore));

        // Then
        assertThat(response.startRank()).isEqualTo(152);
        assertThat(response.endRank()).isEqualTo(138);
        assertThat(response.rankUp()).isEqualTo(14);
        assertThat(response.oldScore()).isEqualTo(4090);
        assertThat(response.newScore()).isEqualTo(4210);
        assertThat(response.rangeStart()).isEqualTo(135);
        assertThat(response.rangeEnd()).isEqualTo(155);

        verify(rankingRepository).addScore(weekStart, userId, gainedScore, "testUser", "sf");
    }

    @Test
    @DisplayName("랭킹 결과 처리 성공 - 신규 사용자 첫 점수 등록")
    void processResultNewUser() {
        // Given
        Long userId = 1L;
        int gainedScore = 100;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        User user = createUser(userId, "newUser", UserImageCategory.comedy);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        given(rankingRepository.getScore(weekStart, userId)).willReturn(null);
        given(rankingRepository.getMyRank(weekStart, userId))
                .willReturn(null)  // before (not in ranking)
                .willReturn(50);   // after
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(49);

        given(rankingRepository.getRankRangeEntries(eq(weekStart), any(Integer.class), any(Integer.class)))
                .willReturn(List.of());

        // When
        RankingResultResponse response = rankingService.processResult(userId, new RankingResultRequest(gainedScore));

        // Then
        assertThat(response.startRank()).isEqualTo(50); // totalParticipants + 1
        assertThat(response.endRank()).isEqualTo(50);
        assertThat(response.rankUp()).isEqualTo(0);
        assertThat(response.oldScore()).isEqualTo(0);
        assertThat(response.newScore()).isEqualTo(100);

        verify(rankingRepository).addScore(weekStart, userId, gainedScore, "newUser", "comedy");
    }

    @Test
    @DisplayName("랭킹 결과 처리 실패 - 사용자 없음")
    void processResultUserNotFound() {
        // Given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rankingService.processResult(userId, new RankingResultRequest(100)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("랭킹 결과 처리 성공 - 구간 데이터 정확성 검증")
    void processResultRankingRange() {
        // Given
        Long userId = 1L;
        int gainedScore = 50;
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        User user = createUser(userId, "rangeUser", UserImageCategory.thriller);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        given(rankingRepository.getScore(weekStart, userId)).willReturn(1000.0);
        given(rankingRepository.getMyRank(weekStart, userId))
                .willReturn(10)
                .willReturn(8);
        given(rankingRepository.getTotalParticipants(weekStart)).willReturn(100);

        ScoredEntry<String> entry1 = createScoredEntry("5", 1200.0);
        ScoredEntry<String> entry2 = createScoredEntry("6", 1150.0);
        ScoredEntry<String> entry3 = createScoredEntry("1", 1050.0);
        ScoredEntry<String> entry4 = createScoredEntry("10", 950.0);
        given(rankingRepository.getRankRangeEntries(weekStart, 4, 12))
                .willReturn(List.of(entry1, entry2, entry3, entry4));
        given(rankingRepository.getUserInfos(eq(weekStart), any()))
                .willReturn(Map.of(
                        "5", "user5:CAT",
                        "6", "user6:DOG",
                        "1", "rangeUser:thriller",
                        "10", "user10:BIRD"
                ));

        // When
        RankingResultResponse response = rankingService.processResult(userId, new RankingResultRequest(gainedScore));

        // Then
        assertThat(response.rangeStart()).isEqualTo(5);  // endRank(8) - 3
        assertThat(response.rangeEnd()).isEqualTo(13);   // startRank(10) + 3
        assertThat(response.rankingRange()).hasSize(4);
        assertThat(response.rankingRange().get(0).rank()).isEqualTo(5);
        assertThat(response.rankingRange().get(0).nickname()).isEqualTo("user5");
    }

    private User createUser(Long id, String nickname, UserImageCategory imageCategory) {
        User user = new User(nickname + "@test.com");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "imageCategory", imageCategory);
        user.setNickname(nickname);
        return user;
    }
}
