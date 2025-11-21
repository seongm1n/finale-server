package com.finale.finale.ranking.controller;

import com.finale.finale.ranking.dto.request.RankingResultRequest;
import com.finale.finale.ranking.dto.response.MyRankingResponse;
import com.finale.finale.ranking.dto.response.RankingResponse;
import com.finale.finale.ranking.dto.response.RankingResultResponse;
import com.finale.finale.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/result")
    public ResponseEntity<RankingResultResponse> submitResult(
            @AuthenticationPrincipal Long userId,
            @RequestBody RankingResultRequest request
    ) {
        RankingResultResponse response = rankingService.processResult(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MyRankingResponse> getMyRanking(@AuthenticationPrincipal Long userId) {
        MyRankingResponse response = rankingService.getMyRanking(userId);
        return ResponseEntity.ok(response);
    }
}
