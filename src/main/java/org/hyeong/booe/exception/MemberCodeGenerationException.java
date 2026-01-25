package org.hyeong.booe.exception;

public class MemberCodeGenerationException extends BusinessException {
    public MemberCodeGenerationException() {
        super(ErrorCode.MEMBER_CODE_GENERATION_FAILED);
    }
}
