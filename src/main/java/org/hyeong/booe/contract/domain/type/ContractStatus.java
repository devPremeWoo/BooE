package org.hyeong.booe.contract.domain.type;

public enum ContractStatus {

    DRAFT,              // 임시저장
    REVIEW_REQUESTED,   // 임대인이 임차인에게 확인 요청
    LESSEE_CONFIRMED,   // 임차인 확인 후 정보 입력 시작
    INFO_COLLECTING,    // 양측 정보 입력 중
    FINAL_REVIEW,       // 양측 정보 완료 → 최종 확인 요청
    PAYMENT_PENDING,    // 양측 동의 → 결제 요청
    PAYMENT_COMPLETED,  // 결제 완료
    SIGN_REQUESTED,     // 모두사인 서명 요청
    SIGNED              // 서명 완료
}
