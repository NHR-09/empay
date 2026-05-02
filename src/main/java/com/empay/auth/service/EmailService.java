package com.empay.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTempPassword(String toEmail, String name, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your EmpPay Account - Temporary Password");
        message.setText(
            "Hello " + name + ",\n\n" +
            "Your account has been created.\n" +
            "Temporary Password: " + tempPassword + "\n\n" +
            "Please login and change your password immediately.\n\n" +
            "Regards,\nEmpPay Team"
        );
        mailSender.send(message);
    }
}
