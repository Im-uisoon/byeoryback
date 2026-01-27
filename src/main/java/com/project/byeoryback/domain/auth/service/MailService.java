package com.project.byeoryback.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final GmailService gmailService;
    private final Map<String, String> verificationStorage = new ConcurrentHashMap<>();

    // 인증번호 생성 (6자리 숫자)
    public String createNumber() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
    }

    // 메일 내용 생성
    private String createBody(String number) {
        String body = "";
        body += "<h3 style='color: #333333; font-size: 16px; font-weight: normal;'>요청하신 인증 번호입니다.</h3>";
        body += "<div style='background-color: #f9f9f9; padding: 30px; margin: 20px 0; text-align: center; border-radius: 10px;'>";
        body += "<span style='font-size: 32px; font-weight: bold; color: #4A90E2; letter-spacing: 5px;'>" + number
                + "</span>";
        body += "</div>";
        body += "<p style='font-size: 14px; color: #666666;'>인증 번호를 입력창에 정확히 입력해 주세요. 감사합니다.</p>";
        body += "</div>";
        return body;
    }

    // 메일 발송
    public String sendSimpleMessage(String sendEmail) throws MessagingException {
        String number = createNumber();
        String body = createBody(number);
        try {
            gmailService.sendEmail(sendEmail, "[벼리] 이메일 인증 번호 안내", body);
            verificationStorage.put(sendEmail, number); // 메모리에 저장
        } catch (Exception e) {
            log.error("메일 발송 오류", e);
            throw new IllegalArgumentException("메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
        return number;
    }

    // 인증번호 검증
    public boolean verifyEmailCode(String email, String code) {
        String storedCode = verificationStorage.get(email);
        if (storedCode != null && storedCode.equals(code)) {
            verificationStorage.remove(email); // 인증 성공 시 삭제
            return true;
        }
        return false;
    }

    public void sendPinCode(String to, String code) {
        try {
            String body = "<h3 style='color: #333333;'>PIN Code: " + code + "</h3>";
            gmailService.sendEmail(to, "[벼리] PIN 코드 안내", body);
        } catch (Exception e) {
            log.error("PIN 메일 발송 오류", e);
            throw new IllegalArgumentException("메일 발송 중 오류가 발생했습니다.");
        }
    }
}
