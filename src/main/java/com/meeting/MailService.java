package com.meeting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingNotification(String toEmail, String username, String title, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("3051490771@qq.com");
            message.setTo(toEmail);
            message.setSubject("【会议室系统】" + title);
            message.setText("亲爱的 " + username + "：\n\n" + content + "\n\n祝您工作愉快！\n\n智能会议室预约系统");
            mailSender.send(message);
            System.out.println("邮件发送成功：" + toEmail);
        } catch (Exception e) {
            System.err.println("邮件发送失败：" + e.getMessage());
        }
    }
}