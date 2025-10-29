package com.finale.finale.auth.dto.response;

import com.finale.finale.auth.domain.Role;

public record UserResponse(
    Long id,
    String email,
    String nickname,
    Role role,
    Integer abilityScore,
    Integer bookReadCount,
    boolean needsNickname
) {
}
