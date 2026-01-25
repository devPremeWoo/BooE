package org.hyeong.booe.member.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemberControllerTest {

    @Test
    void 회원가입_성공_테스트() {

        String requestBody = """
        {
          "memberId": "hyeong01",
          "password": "Abcd1234!!",
          "password2": "Abcd1234!!",
          "name": "홍길동",
          "birthDate": "1998-04-12",
          "email": "test@example.com",
          "phoneNum": "01012345678"
        }
        """;


    }

}