package com.campusfind.service;

import com.campusfind.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            return;
        }

        if (mailSender == null || mailUsername == null || mailUsername.isBlank()) {
            log.info("[MOCK EMAIL] To: {} | Subject: {} | Body snippet: {}",
                    to, subject, htmlBody.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername, "CampusFind");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email successfully sent to: {} [Subject: {}]", to, subject);
        } catch (Exception ex) {
            log.warn("Failed to send real email to {} due to: {}. Logging email content instead.\nSubject: {}\nBody: {}",
                    to, ex.getMessage(), subject, htmlBody);
        }
    }

    public void sendMatchNotification(User recipient, String itemTitle, Long matchId) {
        if (recipient == null || !recipient.isEmailNotifications()) {
            return;
        }
        String subject = "CampusFind: New Match Found for " + itemTitle;
        String body = """
            <div style="font-family: Arial, sans-serif; padding: 16px; color: #333;">
              <h2 style="color: #2563eb;">CampusFind Potential Match</h2>
              <p>Hello %s,</p>
              <p>We found a high-confidence match for your reported item: <strong>%s</strong>.</p>
              <p>Log in to CampusFind to verify the item and connect securely with the other student.</p>
              <p style="color: #64748b; font-size: 12px; margin-top: 24px;">CampusFind - Privacy First Lost & Found Platform</p>
            </div>
            """.formatted(recipient.getFullName(), itemTitle);

        sendEmail(recipient.getEmail(), subject, body);
    }

    public void sendClaimSubmittedNotification(User finder, String itemTitle, Long matchId) {
        if (finder == null || !finder.isEmailNotifications()) {
            return;
        }
        String subject = "CampusFind: New Claim Answer Submitted for " + itemTitle;
        String body = """
            <div style="font-family: Arial, sans-serif; padding: 16px; color: #333;">
              <h2 style="color: #2563eb;">Claim Answer Received</h2>
              <p>Hello %s,</p>
              <p>A student has submitted an answer to your verification question for found item: <strong>%s</strong>.</p>
              <p>Please log in to review the answer and approve or reject the claim.</p>
              <p style="color: #64748b; font-size: 12px; margin-top: 24px;">CampusFind - Privacy First Lost & Found Platform</p>
            </div>
            """.formatted(finder.getFullName(), itemTitle);

        sendEmail(finder.getEmail(), subject, body);
    }

    public void sendClaimDecisionNotification(User owner, String itemTitle, boolean approved, Long matchId) {
        if (owner == null || !owner.isEmailNotifications()) {
            return;
        }
        String decisionText = approved ? "Approved! In-app chat is now unlocked." : "Rejected by the finder.";
        String subject = "CampusFind: Claim " + (approved ? "Approved" : "Rejected") + " for " + itemTitle;
        String body = """
            <div style="font-family: Arial, sans-serif; padding: 16px; color: #333;">
              <h2 style="color: %s;">Claim Decision: %s</h2>
              <p>Hello %s,</p>
              <p>Your ownership claim for item <strong>%s</strong> was <strong>%s</strong>.</p>
              <p>%s</p>
              <p style="color: #64748b; font-size: 12px; margin-top: 24px;">CampusFind - Privacy First Lost & Found Platform</p>
            </div>
            """.formatted(
                approved ? "#16a34a" : "#dc2626",
                approved ? "Approved" : "Rejected",
                owner.getFullName(),
                itemTitle,
                approved ? "Approved" : "Rejected",
                approved ? "You can now safely chat with the finder to arrange handover." : "You may review your post details or try another claim attempt."
            );

        sendEmail(owner.getEmail(), subject, body);
    }

    public void sendItemReturnedNotification(User recipient, String itemTitle, Long matchId) {
        if (recipient == null || !recipient.isEmailNotifications()) {
            return;
        }
        String subject = "CampusFind: Item Marked Returned - " + itemTitle;
        String body = """
            <div style="font-family: Arial, sans-serif; padding: 16px; color: #333;">
              <h2 style="color: #16a34a;">Item Successfully Returned! 🎉</h2>
              <p>Hello %s,</p>
              <p>Item <strong>%s</strong> has been marked as returned.</p>
              <p>Thank you for keeping our college campus helpful, honest, and connected!</p>
              <p style="color: #64748b; font-size: 12px; margin-top: 24px;">CampusFind - Privacy First Lost & Found Platform</p>
            </div>
            """.formatted(recipient.getFullName(), itemTitle);

        sendEmail(recipient.getEmail(), subject, body);
    }
}
