package com.example.demo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public boolean sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("二手房交易平台注册验证码");
        message.setText("您好！\n\n您正在注册二手房交易平台账号，本次验证码为：" + code +
                "。验证码有效期为10分钟，请尽快完成注册。若非本人操作，请忽略此邮件。\n\n（此邮件由系统自动发送，请勿直接回复）");
        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            log.error("Failed to send verification email to {}", to, ex);
            return false;
        }
    }
}
