package com.aiworkforce.core.email;

import com.aiworkforce.identity.dto.EmployeeInvitationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.email.invitation.enabled:false}")
    private boolean invitationEmailEnabled;

    @Value("${app.email.from:no-reply@forep.local}")
    private String fromAddress;

    @Value("${app.email.invitation.subject:Activate your Project Intelligence account}")
    private String invitationSubject;

    public void sendEmployeeInvitation(EmployeeInvitationResponse invitation) {
        if (invitation == null || invitation.getEmail() == null || invitation.getActivationLink() == null) {
            log.warn("Skipping employee invitation email because invitation data is incomplete");
            return;
        }

        if (!invitationEmailEnabled) {
            log.info("Employee invitation email disabled. Activation link for {}: {}", invitation.getEmail(), invitation.getActivationLink());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Employee invitation email enabled but JavaMailSender is not configured. Activation link for {}: {}",
                    invitation.getEmail(), invitation.getActivationLink());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(invitation.getEmail());
        message.setSubject(invitationSubject);
        message.setText(buildInvitationBody(invitation));
        mailSender.send(message);
        log.info("Employee invitation email sent to {}", invitation.getEmail());
    }

    private String buildInvitationBody(EmployeeInvitationResponse invitation) {
        return "Your account has been prepared for the AI-powered Project Intelligence Dashboard.\n\n"
                + "Activate your account using this link:\n"
                + invitation.getActivationLink()
                + "\n\nIf you did not expect this invitation, ignore this email.";
    }
}
