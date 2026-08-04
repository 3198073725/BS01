package com.vidsprout.modules.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.from:VidSprout <noreply@vidsprout.com>}")
    private String fromAddress;
    
    @Value("${SITE_URL:http://localhost:3000}")
    private String siteUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String username, String uid, String token) {
        String verifyUrl = String.format("%s/verify-email?uid=%s&token=%s", siteUrl, uid, token);
        String subject = "VidSprout - 邮箱验证";
        String content = String.format(
            "你好 %s，\n\n欢迎加入 VidSprout！请点击以下链接验证你的邮箱：\n\n%s\n\n如果不是你注册的请忽略此邮件。",
            username, verifyUrl
        );
        sendSimpleEmail(to, subject, content);
    }

    public void sendPasswordResetEmail(String to, String username, String uid, String token) {
        String resetUrl = String.format("%s/reset-password?uid=%s&token=%s", siteUrl, uid, token);
        String subject = "VidSprout - 密码重置";
        String content = String.format(
            "你好 %s，\n\n你请求重置 VidSprout 账号密码。请点击以下链接设置新密码：\n\n%s\n\n" +
            "如果不是你请求的请忽略此邮件，你的密码不会改变。",
            username, resetUrl
        );
        sendSimpleEmail(to, subject, content);
    }

    public void sendLoginCodeEmail(String to, String username, String code) {
        String subject = "VidSprout - 登录验证码";
        String content = String.format(
            "你好 %s，\n\n你的登录验证码是：%s\n\n验证码 5 分钟内有效，请勿分享给他人。",
            username, code
        );
        sendSimpleEmail(to, subject, content);
    }

    private void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("邮件发送失败 to={} subject={}: {}", to, subject, e.getMessage());
        }
    }

    public void sendPlainText(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}
