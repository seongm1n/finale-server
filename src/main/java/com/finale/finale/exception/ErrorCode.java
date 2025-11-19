package com.finale.finale.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "원서를 찾을 수 없습니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "퀴즈를 찾을 수 없습니다."),
    SENTENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "문장을 찾을 수 없습니다."),
    UNKNOWN_WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "모르는 단어를 찾을 수 없습니다."),

    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    ABILITY_ALREADY_INITIALIZED(HttpStatus.BAD_REQUEST, "능력치가 이미 초기화되어 있습니다."),
    BOOK_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 원서입니다."),
    BOOK_USER_MISMATCH(HttpStatus.FORBIDDEN, "원서에 접근할 수 있는 권한이 없습니다."),
    BOOK_NOT_READY(HttpStatus.BAD_REQUEST, "아직 생성된 글이 없습니다."),
    BOOK_ARE_ENOUGH(HttpStatus.BAD_REQUEST, "이미 원서가 충분합니다."),
    BOOK_GENERATION_IN_PROGRESS(HttpStatus.BAD_REQUEST, "원서 생성이 이미 진행 중입니다."),
    BOOK_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료되지 않은 원서입니다."),
    QUIZ_BOOK_MISMATCH(HttpStatus.FORBIDDEN, "퀴즈가 해당 원서에 속하지 않습니다."),

    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서비스와의 통신에 실패했습니다."),
    AI_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서비스로부터 잘못된 응답을 받았습니다."),

    INVALID_SORT_PARAMETER(HttpStatus.BAD_REQUEST, "유효하지 않은 정렬 파라미터입니다."),
    INVALID_CATEGORY_PARAMETER(HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리 파라미터입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    NICKNAME_SAME_AS_BEFORE(HttpStatus.BAD_REQUEST, "이전 닉네임과 동일합니다."),

    PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 잘못되었습니다."),

    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
