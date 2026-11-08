package com.webridge.auth.mail;

import com.webridge.config.WebridgeProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final Optional<JavaMailSender> mailSender;
    private final SpringTemplateEngine templateEngine;
    private final WebridgeProperties props;

    public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context(Locale.KOREAN, variables);
            String html = templateEngine.process(templateName, context);

            if (mailSender.isPresent()) {
                MimeMessage message = mailSender.get().createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.get().send(message);
                log.info("Sent email [{}] to {}", subject, to);
            } else {
                log.warn("JavaMailSender bean not available, skipped sending email to {}", to);
            }
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
