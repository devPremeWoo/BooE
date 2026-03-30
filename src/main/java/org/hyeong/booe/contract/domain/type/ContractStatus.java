package org.hyeong.booe.contract.domain.type;

public enum ContractStatus {

    DRAFT,              // 임시저장
    REVIEW_REQUESTED,   // 임대인이 임차인에게 확인 요청
    LESSEE_CONFIRMED,   // 임차인 동의 완료
    SIGNED              // 최종 서명(PDF 인증) 완료
}
