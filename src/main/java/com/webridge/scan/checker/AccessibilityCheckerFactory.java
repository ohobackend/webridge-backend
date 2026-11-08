package com.webridge.scan.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessibilityCheckerFactory {

    private final CheckerScripts scripts;
    private final AltTextAiClient aiClient;

    public AccessibilityChecker create(String url) {
        return new AccessibilityChecker(url, scripts, aiClient);
    }
}
