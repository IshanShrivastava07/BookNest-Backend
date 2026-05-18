package com.booknest.auth_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtp(String to, String otp) {
        String subject = "Verify your account - Your BookNest verification code is " + otp;
        
        String plainText = "Hello from BookNest!\n\n" +
                          "Thank you for joining our community of book lovers. To complete your registration and ensure the security of your account, please use the following verification code:\n\n" +
                          "Verification Code: " + otp + "\n\n" +
                          "This code is valid for 5 minutes. If you did not initiate this request, someone may have entered your email address by mistake. In that case, you can safely ignore this message; no further action is required.\n\n" +
                          "Happy Reading,\nThe BookNest Team";
                          
        String htmlContent = 
            "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px; color: #333; line-height: 1.6;'>" +
            "  <div style='text-align: center; margin-bottom: 40px;'>" +
            "    <h1 style='color: #D4AF37; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px;'>BookNest</h1>" +
            "    <p style='color: #888; font-size: 14px; margin-top: 5px;'>Your personal gateway to a world of stories</p>" +
            "  </div>" +
            "  " +
            "  <div style='background-color: #ffffff; border: 1px solid #eee; border-radius: 12px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);'>" +
            "    <h2 style='font-size: 20px; color: #111; margin-top: 0;'>Verify your email address</h2>" +
            "    <p>Thanks for signing up for BookNest! We're excited to have you on board. To get started, please enter the following verification code in the app:</p>" +
            "    " +
            "    <div style='background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 8px; padding: 25px; margin: 30px 0; text-align: center;'>" +
            "      <span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #8a6d3b;'>" + otp + "</span>" +
            "    </div>" +
            "    " +
            "    <p style='font-size: 14px; color: #666;'>This code is unique to your registration and will expire in <b>5 minutes</b>. For your security, please do not share this code with anyone.</p>" +
            "  </div>" +
            "  " +
            "  <div style='margin-top: 40px; text-align: center;'>" +
            "    <p style='font-size: 13px; color: #999;'>" +
            "      If you didn't create an account with BookNest, you can safely ignore this email.<br/><br/>" +
            "      &copy; 2026 BookNest Inc. | 123 Library Lane, Reading City" +
            "    </p>" +
            "  </div>" +
            "</div>";

        sendHtmlMail(to, subject, htmlContent, plainText);
    }

    private void sendHtmlMail(String to, String subject, String htmlBody, String plainText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true flag indicates multipart message (HTML + Plain Text)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, "BookNest");
            helper.setTo(to);
            helper.setSubject(subject);
            // Set both HTML and Plain Text
            helper.setText(plainText, htmlBody);
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {} subject=\"{}\" — Cause: {}", to, subject, e.getMessage());
            sendSimpleMail(to, subject, plainText);
        }
    }

    private void sendSimpleMail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Simple mail fallback also failed for {}: {}", to, e.getMessage());
        }
    }

    public void sendMail(String to, String subject, String body) {
        sendOtp(to, body); // For registration flow which calls this
    }

    public void sendPasswordResetOtp(String to, String otp) {
        String subject = "Reset your BookNest password";
        
        String plainText = "Hello from BookNest!\n\n" +
                          "You have requested to reset your password. Please use the following code to reset it:\n\n" +
                          "Reset Code: " + otp + "\n\n" +
                          "This code is valid for 5 minutes. If you did not request a password reset, please ignore this email.\n\n" +
                          "Happy Reading,\nThe BookNest Team";
                          
        String htmlContent = 
            "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px; color: #333; line-height: 1.6;'>" +
            "  <div style='text-align: center; margin-bottom: 40px;'>" +
            "    <h1 style='color: #D4AF37; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px;'>BookNest</h1>" +
            "  </div>" +
            "  " +
            "  <div style='background-color: #ffffff; border: 1px solid #eee; border-radius: 12px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.05);'>" +
            "    <h2 style='font-size: 20px; color: #111; margin-top: 0;'>Reset your password</h2>" +
            "    <p>You recently requested to reset your password for your BookNest account. Use the code below to proceed:</p>" +
            "    " +
            "    <div style='background-color: #fcf8e3; border: 1px solid #faebcc; border-radius: 8px; padding: 25px; margin: 30px 0; text-align: center;'>" +
            "      <span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #8a6d3b;'>" + otp + "</span>" +
            "    </div>" +
            "    " +
            "    <p style='font-size: 14px; color: #666;'>This code will expire in <b>5 minutes</b>.</p>" +
            "  </div>" +
            "</div>";

        sendHtmlMail(to, subject, htmlContent, plainText);
    }
}
