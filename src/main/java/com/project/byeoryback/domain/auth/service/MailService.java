package com.project.byeoryback.domain.auth.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private static final String APPLICATION_NAME = "byeoryback";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.refresh.token}")
    private String refreshToken;

    private final Map<String, String> verificationStorage = new ConcurrentHashMap<>();

    private Gmail getGmailService() throws IOException, java.security.GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Credential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // 인증번호 생성 (6자리 숫자)
    public String createNumber() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
    }

    // 이메일 본문 생성
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

    // MimeMessage 생성
    public MimeMessage createMimeMessage(String to, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress("me"));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(bodyText, "text/html; charset=utf-8");
        return email;
    }

    // Gmail API Message 생성
    public Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = com.google.api.client.util.Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    // 메일 발송
    public String sendSimpleMessage(String sendEmail) {
        String number = createNumber();
        try {
            String body = createBody(number);
            MimeMessage mimeMessage = createMimeMessage(sendEmail, "[벼리] 이메일 인증 번호 안내", body);
            Message message = createMessageWithEmail(mimeMessage);

            Gmail service = getGmailService();
            service.users().messages().send("me", message).execute();

            verificationStorage.put(sendEmail, number); // 메모리에 저장
        } catch (Exception e) {
            log.error("메일 발송 오류", e);
            throw new IllegalArgumentException("메일 발송 중 오류가 발생했습니다.");
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
            // PIN 코드 발송 로직도 동일하게 Gmail API 사용
            String body = "";
            body += "<h3 style='color: #333333; font-size: 16px; font-weight: normal;'>요청하신 핀 코드입니다.</h3>";
            body += "<div style='background-color: #f9f9f9; padding: 30px; margin: 20px 0; text-align: center; border-radius: 10px;'>";
            body += "<span style='font-size: 32px; font-weight: bold; color: #4A90E2; letter-spacing: 5px;'>" + code
                    + "</span>";
            body += "</div>";
            body += "<p style='font-size: 14px; color: #666666;'>핀 코드를 입력창에 정확히 입력해 주세요. 감사합니다.</p>";
            body += "</div>";

            MimeMessage mimeMessage = createMimeMessage(to, "[벼리] 핀 코드 안내", body);
            Message message = createMessageWithEmail(mimeMessage);

            Gmail service = getGmailService();
            service.users().messages().send("me", message).execute();
        } catch (Exception e) {
            log.error("PIN 메일 발송 오류", e);
            throw new IllegalArgumentException("메일 발송 중 오류가 발생했습니다.");
        }
    }
}
