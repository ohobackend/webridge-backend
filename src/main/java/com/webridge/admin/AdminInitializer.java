package com.webridge.admin;

import com.webridge.user.entity.User;
import com.webridge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    public static final String PRIMARY_ADMIN_USERNAME = "inableit";
    public static final String PRIMARY_ADMIN_EMAIL = "inableit@inableit.com";
    public static final String PRIMARY_ADMIN_PASSWORD = "inableit2025";

    private static final String SECONDARY_ADMIN_EMAIL = "youngho7482@naver.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initPrimaryAdmin();
        initSecondaryAdmin();
    }

    private void initPrimaryAdmin() {
        try {
            // inableit@inableit.com 또는 inableit 검색
            User admin = userRepository.findByEmail(PRIMARY_ADMIN_EMAIL)
                .or(() -> userRepository.findByEmail(PRIMARY_ADMIN_USERNAME))
                .orElse(null);

            if (admin != null) {
                admin.setName(PRIMARY_ADMIN_USERNAME);
                admin.setPassword(passwordEncoder.encode(PRIMARY_ADMIN_PASSWORD));
                admin.setAdmin(true);
                admin.setSuperuser(true);
                admin.setActive(true);
                userRepository.save(admin);
                log.info("Primary admin account synchronized: ID '{}' (PW: '{}')", PRIMARY_ADMIN_USERNAME, PRIMARY_ADMIN_PASSWORD);
            } else {
                User newAdmin = User.builder()
                    .email(PRIMARY_ADMIN_EMAIL)
                    .name(PRIMARY_ADMIN_USERNAME)
                    .password(passwordEncoder.encode(PRIMARY_ADMIN_PASSWORD))
                    .provider("email")
                    .isActive(true)
                    .isAdmin(true)
                    .isSuperuser(true)
                    .serviceTermsAgreed(true)
                    .serviceTermsAgreedAt(OffsetDateTime.now())
                    .ageVerificationAgreed(true)
                    .ageVerificationAgreedAt(OffsetDateTime.now())
                    .privacyPolicyAgreed(true)
                    .privacyPolicyAgreedAt(OffsetDateTime.now())
                    .marketingAgreed(false)
                    .isSocialSignupComplete(true)
                    .build();

                userRepository.save(newAdmin);
                log.info("Primary admin account created: ID '{}' (PW: '{}')", PRIMARY_ADMIN_USERNAME, PRIMARY_ADMIN_PASSWORD);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize primary admin user: {}", e.getMessage());
        }
    }

    private void initSecondaryAdmin() {
        try {
            userRepository.findByEmail(SECONDARY_ADMIN_EMAIL).ifPresent(user -> {
                if (!user.isAdmin() || !user.isActive()) {
                    user.setAdmin(true);
                    user.setActive(true);
                    userRepository.save(user);
                    log.info("Admin privileges ensured for {}", SECONDARY_ADMIN_EMAIL);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to ensure secondary admin user: {}", e.getMessage());
        }
    }
}
