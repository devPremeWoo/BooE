package org.hyeong.booe.global.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    public void send(String fcmToken, String title, String body, String contractId) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("contractId", contractId)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] 전송 실패 - token: {}, error: {}", fcmToken, e.getMessage());
        }
    }

    public void sendToAll(List<String> fcmTokens, String title, String body, String contractId) {
        fcmTokens.forEach(token -> send(token, title, body, contractId));
    }
}
