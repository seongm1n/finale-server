package com.finale.finale.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Random;

@MappedSuperclass
@Getter
public abstract class ReviewableItem {

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    protected void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public void nextReviewSetting() {
        int[] intervals = {1, 3, 7, 14, 30, 75, 188, 469, 1173, 2933};
        Random random = new Random();

        int index = Math.min(reviewCount, intervals.length - 1);
        int base = intervals[index];
        int range = (int) Math.round(base * 0.1);
        int offset = range > 0 ? random.nextInt(2 * range + 1) - range : 0;

        this.nextReviewDate = LocalDate.now().plusDays(Math.max(base + offset, 0));
        this.reviewCount = reviewCount + 1;
    }

}
