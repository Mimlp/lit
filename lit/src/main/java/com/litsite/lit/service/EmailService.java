package com.litsite.lit.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    @Autowired
    private JavaMailSender emailSender;

    public void sendVerificationEmail(String to, String subject, String text) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            emailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            emailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    public void sendContentModerationNotice(String to, String contentType, String contentTitle, String reason) {
        String subject = "Ваш контент был удалён модератором";
        String html = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Здравствуйте!</h2>
                    <p>Ваш %s <strong>"%s"</strong> была удалена модератором платформы.</p>
                    <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; margin: 20px 0;">
                        <strong>Причина:</strong><br>%s
                    </div>
                    <p>Если вы считаете, что это ошибка, напишите в поддержку: kuznetsovako2022@gmail.com</p>
                    <hr style="margin: 30px 0;"/>
                    <p style="font-size: 12px; color: #666;">Команда LitSite</p>
                </body>
                </html>
                """, contentType, contentTitle, reason);

        sendHtmlEmail(to, subject, html);
    }

    public void sendAccountDisabledNotice(String to, String username, String reason) {
        String subject = "Ваш аккаунт был временно ограничен";
        String html = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2>Здравствуйте, %s!</h2>
                    <p>Ваш аккаунт на платформе LitSite был временно ограничен.</p>
                    <div style="background: #f8d7da; border-left: 4px solid #dc3545; padding: 12px; margin: 20px 0;">
                        <strong>Причина:</strong><br>%s
                    </div>
                    <p>Для обжалования напишите в поддержку: kuznetsovako2022@gmail.com</p>
                    <hr style="margin: 30px 0;"/>
                    <p style="font-size: 12px; color: #666;">Команда LitSite</p>
                </body>
                </html>
                """, username, reason);

        sendHtmlEmail(to, subject, html);
    }
}