package org.hyeong.booe.contract.domain.type;

public enum ContractStatus {

    DRAFT,              // 임대인 계약서 작성 중
    REVIEW_REQUESTED,   // 임차인에게 확인 요청 발송
    LESSEE_SUBMITTED,   // 임차인 정보 입력 완료
    PAYMENT_PENDING,    // 임대인 계약금 확인 + 영수자 정보 입력 → 결제 요청
    PAYMENT_COMPLETED,  // 결제 완료
    SIGN_REQUESTED,     // 모두사인 서명 요청 중
    SIGNED              // 서명 완료 → PDF S3 저장
}
