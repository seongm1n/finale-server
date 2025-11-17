package com.finale.finale.auth.domain;

import java.util.Random;

public enum UserImageCategory {
    sf, comedy, romance, moral, thriller, adventure;

    private static final Random RANDOM = new Random();

    public static UserImageCategory getRandomCategory() {
        UserImageCategory[] categories = values();
        return categories[RANDOM.nextInt(categories.length)];
    }
}
