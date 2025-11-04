package com.finale.finale.auth.domain;

import com.finale.finale.exception.CustomException;
import com.finale.finale.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 엔티티 테스트")
public class UserTest {

    @Test
    @DisplayName("능력 점수 초기화 - 성공")
    void initAbilityScore() {
        // Given
        User user = new User("test@example.com");

        // When
        user.initAbilityScore(700);

        // Then
        assertThat(user.getAbilityScore()).isEqualTo(700);
    }

    @Test
    @DisplayName("능력 점수 초기화 - 이미 초기화된 경우 예외")
    void initAbilityScoreAlreadyInitialized() {
        // Given
        User user = new User("test@example.com");
        user.initAbilityScore(700);

        // When & Then
        assertThatThrownBy(() -> user.initAbilityScore(800))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ABILITY_ALREADY_INITIALIZED);
    }

    @Test
    @DisplayName("능력 점수 조정 - 낮은 비율일 때 상승")
    void inclusionScoreIncrease() {
        // Given
        User user = new User("test@example.com");

        // When
        user.inclusionScore(0, 1, 100);

        // Then
        assertThat(user.getAbilityScore()).isGreaterThan(500);
    }

    @Test
    @DisplayName("능력 점수 조정 - 높은 비율일 때 하락")
    void inclusionScoreDecrease() {
        // Given
        User user = new User("test@example.com");

        // When
        user.inclusionScore(0, 7, 100);

        // Then
        assertThat(user.getAbilityScore()).isLessThan(500);
    }

    @Test
    @DisplayName("학습 상태 업데이트 - 연속 학습")
    void learningStatusConsecutive() {
        // Given
        User user = new User("test@example.com");

        // When
        user.learningStatusToday(10);

        // Then
        assertThat(user.getContinuosLearning()).isEqualTo(1);
        assertThat(user.getTodayBooksReadCount()).isEqualTo(1);
        assertThat(user.getTodaySentencesReadCount()).isEqualTo(10);
        assertThat(user.getLastLearnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("학습 상태 업데이트 - 같은 날 학습")
    void learningStatusSameDay() {
        // Given
        User user = new User("test@example.com");
        user.learningStatusToday(10);

        // When
        user.learningStatusToday(15);

        // Then
        assertThat(user.getTodayBooksReadCount()).isEqualTo(2);
        assertThat(user.getTodaySentencesReadCount()).isEqualTo(25);
    }
}
