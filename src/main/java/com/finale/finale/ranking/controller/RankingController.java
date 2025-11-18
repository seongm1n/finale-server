package com.finale.finale.ranking.controller;

import com.finale.finale.ranking.dto.RankingResponse;
import com.finale.finale.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<RankingResponse> getRankings(@AuthenticationPrincipal Long userId) {
        RankingResponse response = rankingService.getRankings(userId);
        return ResponseEntity.ok(response);
    }
}
