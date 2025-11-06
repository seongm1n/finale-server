package com.finale.finale.book.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Random;

@Getter
@RequiredArgsConstructor
public enum BookCategory {
    GENERAL("general", "일반"),
    COMEDY("comedy", "코미디"),
    ROMANCE("romance", "로맨스"),
    MORAL("moral", "교훈"),
    THRILLER("thriller", "스릴러"),
    ADVENTURE("adventure", "모험");

    private final String value;
    private final String koreanName;

    private static final Random RANDOM = new Random();
    private static final BookCategory[] VALUES = values();

    public static BookCategory random() {
        return VALUES[RANDOM.nextInt(VALUES.length)];
    }
}
