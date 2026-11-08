package com.webridge.scan.checker;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class AccessibilityChecker implements AutoCloseable {

    private final String url;
    private final String baseUrl;
    private final CheckerScripts scripts;
    private final AltTextAiClient aiClient;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public AccessibilityChecker(String url, CheckerScripts scripts, AltTextAiClient aiClient) {
        this.url = url;
        this.baseUrl = extractBaseUrl(url);
        this.scripts = scripts;
        this.aiClient = aiClient;
        initBrowser();
    }

    private static String extractBaseUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
            String host = uri.getHost() != null ? uri.getHost() : "";
            int port = uri.getPort();
            if (port > 0 && port != 80 && port != 443) {
                return scheme + "://" + host + ":" + port;
            }
            return scheme + "://" + host;
        } catch (Exception e) {
            return url;
        }
    }

    private void initBrowser() {
        try {
            this.playwright = Playwright.create();
            this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                            "--no-sandbox",
                            "--disable-dev-shm-usage",
                            "--headless=new",
                            "--disable-background-timer-throttling",
                            "--disable-backgrounding-occluded-windows",
                            "--disable-renderer-backgrounding",
                            "--disable-gpu",
                            "--window-size=1920,1080",
                            "--window-position=0,0",
                            "--disable-infobars"
                    )));
            this.context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setExtraHTTPHeaders(Map.of(
                            "Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8",
                            "Accept-Encoding", "gzip, deflate, br",
                            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                    )));
            this.page = context.newPage();
            page.setDefaultTimeout(60_000);
            page.onConsoleMessage(msg -> log.debug("[JS-{}] {}", msg.type(), msg.text()));
            page.onRequestFailed(req -> {
                if (req.failure() != null) {
                    log.debug("[Network] Load failed: {} ({})", req.url(), req.failure());
                }
            });
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(60_000));
        } catch (Exception e) {
            close();
            throw new RuntimeException("Playwright browser initialization failed: " + e.getMessage(), e);
        }
    }

    public Page getPage() {
        return page;
    }

    // -----------------------------------------------------------------------------------------
    // 0. Page Title
    // -----------------------------------------------------------------------------------------
    public String extractPageTitle() {
        try {
            String title = page.title();
            if (title != null && !title.isBlank()) {
                return title.trim();
            }
            return getFallbackTitle();
        } catch (Exception e) {
            return getFallbackTitle();
        }
    }

    private String getFallbackTitle() {
        try {
            URI uri = URI.create(url);
            String domain = uri.getHost();
            return (domain != null && !domain.isBlank()) ? domain : "알 수 없는 페이지";
        } catch (Exception e) {
            return "알 수 없는 페이지";
        }
    }

    // -----------------------------------------------------------------------------------------
    // 1. Basic Language (25번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkBasicLanguage() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String htmlLang = page.locator("html").getAttribute("lang");
            boolean hasLang = htmlLang != null && !htmlLang.trim().isEmpty();

            Map<String, Object> item = new HashMap<>();
            item.put("lang_attribute", hasLang ? htmlLang : "없음");
            item.put("compliant", hasLang);
            item.put("message", hasLang ? "기본 언어가 설정되었습니다." : "HTML lang 속성이 누락되었습니다.");
            results.add(item);
        } catch (Exception e) {
            log.error("checkBasicLanguage failed", e);
            results.add(Map.of("error", "기본 언어 표시 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 2. Labels (19번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkLabels() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<String> inputSelectors = List.of("input", "select", "textarea");
            for (String selector : inputSelectors) {
                List<Locator> inputs = page.locator(selector).all();
                for (Locator inputElement : inputs) {
                    try {
                        String elementId = inputElement.getAttribute("id");
                        boolean hasLabel = false;

                        if (elementId != null && !elementId.isBlank()) {
                            hasLabel = page.locator("label[for=\"" + elementId + "\"]").count() > 0;
                        }

                        if (!hasLabel) {
                            String ariaLabel = inputElement.getAttribute("aria-label");
                            String ariaLabelledby = inputElement.getAttribute("aria-labelledby");
                            hasLabel = (ariaLabel != null && !ariaLabel.isBlank()) ||
                                       (ariaLabelledby != null && !ariaLabelledby.isBlank());
                        }

                        String tagName = (String) inputElement.evaluate("el => el.tagName.toLowerCase()");
                        String elementHtml = (String) inputElement.evaluate("el => el.outerHTML");

                        Map<String, Object> item = new HashMap<>();
                        item.put("input_type", tagName != null ? tagName : "");
                        item.put("element_html", elementHtml != null ? elementHtml : "");
                        item.put("label_present", hasLabel);
                        item.put("message", hasLabel ? "레이블이 제공되었습니다." : "레이블이 제공되지 않았습니다.");
                        results.add(item);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("checkLabels failed", e);
            results.add(Map.of("error", "레이블 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 3. Tables (22번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkTables() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<Locator> tables = page.locator("table").all();
            for (Locator table : tables) {
                try {
                    List<Locator> headers = table.locator("th").all();
                    List<Locator> rows = table.locator("tr").all();
                    String tableHtml = (String) table.evaluate("el => el.outerHTML");
                    boolean caption = table.locator("caption").count() > 0;

                    int headersWithScope = 0;
                    for (Locator header : headers) {
                        String scope = header.getAttribute("scope");
                        if (scope != null && !scope.isBlank()) {
                            headersWithScope++;
                        }
                    }

                    boolean compliant = !headers.isEmpty() && !rows.isEmpty();

                    Map<String, Object> item = new HashMap<>();
                    item.put("headers", headers.size());
                    item.put("rows", rows.size());
                    item.put("element_html", tableHtml != null ? tableHtml : "");
                    item.put("caption", caption);
                    item.put("headers_with_scope", headersWithScope);
                    item.put("compliant", compliant);
                    item.put("message", compliant ? "표 구성 확인 완료" : "표 구성 오류");
                    results.add(item);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("checkTables failed", e);
            results.add(Map.of("error", "표 구조 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 4. Keyboard (7번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkKeyboard() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<String> focusableSelectors = List.of(
                    "a[href]",
                    "button:not([disabled])",
                    "input:not([disabled])",
                    "textarea:not([disabled])",
                    "select:not([disabled])",
                    "[tabindex]:not([tabindex=\"-1\"])"
            );

            for (String selector : focusableSelectors) {
                List<Locator> elements = page.locator(selector).all();
                for (Locator element : elements) {
                    try {
                        String tagName = (String) element.evaluate("el => el.tagName.toLowerCase()");
                        String elementHtml = (String) element.evaluate("el => el.outerHTML");
                        boolean isVisible = element.isVisible();
                        boolean isEnabled = element.isEnabled();
                        boolean accessible = isVisible && isEnabled;

                        Map<String, Object> item = new HashMap<>();
                        item.put("element", tagName != null ? tagName : "");
                        item.put("element_html", elementHtml != null ? elementHtml : "");
                        item.put("accessible", accessible);
                        item.put("message", accessible ? "키보드 접근 가능" : "키보드 접근 불가");
                        results.add(item);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            log.error("checkKeyboard failed", e);
            results.add(Map.of("error", "키보드 접근성 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 5. Markup Errors (32번)
    // -----------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkMarkupErrors() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            // 1. 중복 ID 검사
            List<Map<String, Object>> duplicates = (List<Map<String, Object>>) page.evaluate(scripts.get("markup_duplicate_ids"));
            if (duplicates != null) {
                for (Map<String, Object> item : duplicates) {
                    String id = (String) item.get("id");
                    String html = (String) item.get("html");
                    Map<String, Object> res = new HashMap<>();
                    res.put("error_type", "중복 ID");
                    res.put("error_detail", "중복 ID 발견: " + id);
                    res.put("element_id", id);
                    res.put("element_html", html);
                    res.put("compliant", false);
                    res.put("message", "ID '" + id + "'가 중복 사용되었습니다");
                    results.add(res);
                }
            }

            // 2. 필수 속성 누락 검사
            List<Map<String, Object>> missingAttrs = (List<Map<String, Object>>) page.evaluate(scripts.get("markup_missing_attrs"));
            if (missingAttrs != null) {
                for (Map<String, Object> attrError : missingAttrs) {
                    String type = (String) attrError.get("type");
                    String html = (String) attrError.get("html");
                    Map<String, Object> res = new HashMap<>();
                    res.put("error_type", "필수 속성 누락");
                    res.put("error_detail", type);
                    res.put("element_html", html);
                    res.put("compliant", false);
                    res.put("message", type);
                    results.add(res);
                }
            }

            // 3. 빈 요소 검사
            List<Map<String, Object>> emptyElements = (List<Map<String, Object>>) page.evaluate(scripts.get("markup_empty_elements"));
            if (emptyElements != null) {
                for (Map<String, Object> emptyError : emptyElements) {
                    String type = (String) emptyError.get("type");
                    String html = (String) emptyError.get("html");
                    Map<String, Object> res = new HashMap<>();
                    res.put("error_type", "빈 요소");
                    res.put("error_detail", type);
                    res.put("element_html", html);
                    res.put("compliant", false);
                    res.put("message", type);
                    results.add(res);
                }
            }
        } catch (Exception e) {
            log.error("checkMarkupErrors failed", e);
            results.add(Map.of("error", "마크업 오류 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 6. Headings (6.4.2 - 18번)
    // -----------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkHeadings() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Map<String, Object> pageTitleInfo = (Map<String, Object>) page.evaluate(scripts.get("headings_page_title"));
            List<Map<String, Object>> popupInfo = (List<Map<String, Object>>) page.evaluate(scripts.get("headings_popups"));
            List<Map<String, Object>> frameInfo = (List<Map<String, Object>>) page.evaluate(scripts.get("headings_frames"));
            Map<String, Object> contentHeadingsInfo = (Map<String, Object>) page.evaluate(scripts.get("headings_content"));
            List<Map<String, Object>> specialSymbolInfo = (List<Map<String, Object>>) page.evaluate(scripts.get("headings_special_symbols"));

            if (popupInfo == null) popupInfo = List.of();
            if (frameInfo == null) frameInfo = List.of();
            if (specialSymbolInfo == null) specialSymbolInfo = List.of();

            List<Map<String, Object>> headings = (List<Map<String, Object>>) (contentHeadingsInfo != null ? contentHeadingsInfo.get("headings") : List.of());
            List<Map<String, Object>> sections = (List<Map<String, Object>>) (contentHeadingsInfo != null ? contentHeadingsInfo.get("sections") : List.of());
            if (headings == null) headings = List.of();
            if (sections == null) sections = List.of();

            List<String> issues = new ArrayList<>();
            boolean compliant = true;

            // 1. 페이지 제목 검사
            boolean titleExists = Boolean.TRUE.equals(pageTitleInfo.get("exists"));
            boolean titleEmpty = Boolean.TRUE.equals(pageTitleInfo.get("empty"));
            int titleLength = pageTitleInfo.get("length") instanceof Number n ? n.intValue() : 0;
            String titleText = (String) pageTitleInfo.get("text");

            if (!titleExists || titleEmpty) {
                issues.add("웹 페이지에 제목(title)이 없거나 비어있습니다.");
                compliant = false;
            } else if (titleLength < 3) {
                issues.add("웹 페이지 제목이 너무 짧습니다. 간단명료하게 작성해주세요.");
                compliant = false;
            }

            // 2. 팝업창/iframe 제목 검사
            int popupsWithoutTitle = 0;
            for (Map<String, Object> popup : popupInfo) {
                if (!Boolean.TRUE.equals(popup.get("hasTitle"))) {
                    issues.add(popup.get("type") + " 요소에 제목(title)이 없습니다.");
                    compliant = false;
                    popupsWithoutTitle++;
                }
            }

            // 3. 프레임 제목 검사
            int framesWithoutTitle = 0;
            for (Map<String, Object> frame : frameInfo) {
                if (!Boolean.TRUE.equals(frame.get("hasTitle"))) {
                    issues.add(frame.get("tag") + " 요소에 제목(title)이 없습니다.");
                    compliant = false;
                    framesWithoutTitle++;
                }
            }

            // 4. 콘텐츠 블록 제목 검사
            List<Map<String, Object>> emptyHeadings = headings.stream()
                    .filter(h -> Boolean.TRUE.equals(h.get("empty"))).toList();
            if (!emptyHeadings.isEmpty()) {
                issues.add("빈 제목이 " + emptyHeadings.size() + "개 발견되었습니다.");
                compliant = false;
            }

            List<Map<String, Object>> headingsWithoutContent = headings.stream()
                    .filter(h -> !Boolean.TRUE.equals(h.get("hasBodyContent"))).toList();
            if (!headingsWithoutContent.isEmpty()) {
                issues.add("본문 내용이 없는 제목이 " + headingsWithoutContent.size() + "개 발견되었습니다.");
                compliant = false;
            }

            List<Map<String, Object>> sectionsWithoutHeadings = sections.stream()
                    .filter(s -> Boolean.TRUE.equals(s.get("hasContent")) && !Boolean.TRUE.equals(s.get("hasHeading"))).toList();
            if (!sectionsWithoutHeadings.isEmpty()) {
                issues.add("제목이 없는 콘텐츠 블록(section, article 등)이 " + sectionsWithoutHeadings.size() + "개 발견되었습니다.");
                compliant = false;
            }

            // 5. 특수 기호 반복 사용 검사
            if (!specialSymbolInfo.isEmpty()) {
                issues.add("특수 기호가 반복 사용된 제목이 " + specialSymbolInfo.size() + "개 발견되었습니다.");
                compliant = false;
            }

            // 문제 HTML 추출 (최대 3개)
            String elementHtml = extractProblemElementsHtml(popupInfo, frameInfo, headings, sections);

            Map<String, Object> res = new HashMap<>();
            res.put("guideline", "6.4.2 제목 제공");
            res.put("page_title", Map.of(
                    "exists", titleExists,
                    "text", titleText != null ? titleText : "",
                    "length", titleLength
            ));
            res.put("popup_frames", Map.of(
                    "total", popupInfo.size(),
                    "without_title", popupsWithoutTitle
            ));
            res.put("frames", Map.of(
                    "total", frameInfo.size(),
                    "without_title", framesWithoutTitle
            ));
            res.put("content_headings", Map.of(
                    "total", headings.size(),
                    "empty", emptyHeadings.size(),
                    "without_content", headingsWithoutContent.size()
            ));
            res.put("semantic_sections", Map.of(
                    "total", sections.size(),
                    "without_headings", sectionsWithoutHeadings.size()
            ));
            res.put("special_symbol_issues", specialSymbolInfo.size());
            res.put("compliant", compliant);
            res.put("issues", issues);
            res.put("element_html", elementHtml);
            res.put("message", compliant ? "제목 제공 지침 준수" : "제목 제공 지침 위반");
            res.put("details", Map.of(
                    "popup_info", popupInfo,
                    "frame_info", frameInfo,
                    "headings_info", headings,
                    "sections_info", sections,
                    "special_symbols", specialSymbolInfo
            ));
            results.add(res);
        } catch (Exception e) {
            log.error("checkHeadings failed", e);
            results.add(Map.of("error", "제목 제공 검사 실패: " + e.getMessage(), "compliant", false));
        }
        return results;
    }

    private String extractProblemElementsHtml(List<Map<String, Object>> popupInfo,
                                              List<Map<String, Object>> frameInfo,
                                              List<Map<String, Object>> headings,
                                              List<Map<String, Object>> sections) {
        List<String> problemHtmls = new ArrayList<>();
        for (Map<String, Object> popup : popupInfo) {
            if (!Boolean.TRUE.equals(popup.get("hasTitle")) && popup.get("html") != null) {
                problemHtmls.add((String) popup.get("html"));
            }
        }
        for (Map<String, Object> frame : frameInfo) {
            if (!Boolean.TRUE.equals(frame.get("hasTitle")) && frame.get("html") != null) {
                problemHtmls.add((String) frame.get("html"));
            }
        }
        for (Map<String, Object> heading : headings) {
            if (Boolean.TRUE.equals(heading.get("empty")) && heading.get("html") != null) {
                problemHtmls.add((String) heading.get("html"));
            }
        }
        for (Map<String, Object> section : sections) {
            if (!Boolean.TRUE.equals(section.get("hasHeading")) && section.get("html") != null) {
                problemHtmls.add((String) section.get("html"));
            }
        }
        int limit = Math.min(3, problemHtmls.size());
        return String.join("\n", problemHtmls.subList(0, limit));
    }

    // -----------------------------------------------------------------------------------------
    // 7. Response Time (14번)
    // -----------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkResponseTime() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Map<String, Object> timeoutInfo = (Map<String, Object>) page.evaluate(scripts.get("response_time"));
            if (timeoutInfo == null) timeoutInfo = Map.of();

            List<?> timeouts = timeoutInfo.get("timeouts") instanceof List<?> list ? list : List.of();
            boolean hasShortTimeout = timeouts.stream().anyMatch(t -> {
                if (t instanceof Number n) return n.doubleValue() < 20000;
                return false;
            });
            boolean compliant = !hasShortTimeout;

            List<String> scriptElements = new ArrayList<>();
            try {
                List<Locator> scriptsList = page.locator("script").all();
                for (Locator script : scriptsList) {
                    String scriptHtml = (String) script.evaluate("el => el.outerHTML");
                    if (scriptHtml != null && (scriptHtml.contains("setTimeout") || scriptHtml.contains("setInterval"))) {
                        scriptElements.add(scriptHtml);
                    }
                }
            } catch (Exception ignored) {}

            int shortTimeoutCount = (int) timeouts.stream().filter(t -> {
                if (t instanceof Number n) return n.doubleValue() < 20000;
                return false;
            }).count();

            int limit = Math.min(3, scriptElements.size());
            String htmlStr = String.join("\n", scriptElements.subList(0, limit));

            Map<String, Object> res = new HashMap<>();
            res.put("has_time_limit", Boolean.TRUE.equals(timeoutInfo.get("hasTimeLimit")));
            res.put("short_timeouts", shortTimeoutCount);
            res.put("element_html", htmlStr);
            res.put("compliant", compliant);
            res.put("message", compliant ? "적절한 응답시간 제공" : "20초 미만의 시간 제한 발견");
            results.add(res);
        } catch (Exception e) {
            log.error("checkResponseTime failed", e);
            results.add(Map.of("error", "응답시간 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 8. Flashing (16번)
    // -----------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkFlashing() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Map<String, Object> flashingInfo = (Map<String, Object>) page.evaluate(scripts.get("flashing"));
            if (flashingInfo == null) flashingInfo = Map.of();

            List<Map<String, Object>> flashingElements = (List<Map<String, Object>>) flashingInfo.get("flashingElements");
            boolean hasFlashingScript = Boolean.TRUE.equals(flashingInfo.get("hasFlashingScript"));

            if (flashingElements != null) {
                for (Map<String, Object> element : flashingElements) {
                    String html = (String) element.get("html");
                    String issue = (String) element.getOrDefault("issue", "깜빡임 효과");
                    Number duration = (Number) element.get("duration");

                    Map<String, Object> res = new HashMap<>();
                    res.put("flashing_type", issue);
                    res.put("element_html", html);
                    res.put("duration", duration != null ? duration.intValue() : null);
                    res.put("compliant", false);
                    res.put("message", issue + " 발견");
                    results.add(res);
                }
            }

            if (hasFlashingScript) {
                Map<String, Object> res = new HashMap<>();
                res.put("flashing_type", "JavaScript 깜빡임 효과");
                res.put("element_html", null);
                res.put("duration", null);
                res.put("compliant", false);
                res.put("message", "setInterval을 사용한 opacity/visibility/display 변경 감지");
                results.add(res);
            }
        } catch (Exception e) {
            log.error("checkFlashing failed", e);
            results.add(Map.of("error", "깜빡임 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 9. Video Captions (4번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkVideoCaptions() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<Locator> videos = page.locator("video").all();
            for (Locator video : videos) {
                try {
                    String videoUrl = video.getAttribute("src");
                    String videoHtml = (String) video.evaluate("el => el.outerHTML");

                    List<Locator> tracks = video.locator("track").all();
                    boolean hasCaptions = !tracks.isEmpty();

                    String poster = video.getAttribute("poster");
                    boolean hasPoster = poster != null && !poster.isBlank();

                    boolean hasTranscript = hasCaptions;

                    long descTracks = tracks.stream()
                            .filter(t -> "descriptions".equalsIgnoreCase(t.getAttribute("kind")))
                            .count();
                    boolean hasAudioDescription = descTracks > 0;

                    String ariaLabel = video.getAttribute("aria-label");
                    String ariaLabelledby = video.getAttribute("aria-labelledby");
                    boolean hasAriaLabel = (ariaLabel != null && !ariaLabel.isBlank()) ||
                                           (ariaLabelledby != null && !ariaLabelledby.isBlank());

                    boolean compliant = hasCaptions || hasTranscript || hasAudioDescription;

                    Map<String, Object> item = new HashMap<>();
                    item.put("video_url", videoUrl != null ? videoUrl : "");
                    item.put("element_html", videoHtml != null ? videoHtml : "");
                    item.put("has_thumbnail", hasPoster);
                    item.put("has_transcript", hasTranscript);
                    item.put("has_audio_description", hasAudioDescription);
                    item.put("has_aria_label", hasAriaLabel);
                    item.put("compliant", compliant);
                    item.put("message", compliant ? "자막/대본 제공됨" : "자막/대본 제공 필요");
                    results.add(item);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("checkVideoCaptions failed", e);
            results.add(Map.of("error", "비디오 자막 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 10. Video Autoplay (5번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkVideoAutoplay() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<Locator> videos = page.locator("video").all();
            for (Locator video : videos) {
                try {
                    String videoUrl = video.getAttribute("src");
                    String videoHtml = (String) video.evaluate("el => el.outerHTML");

                    String autoplay = video.getAttribute("autoplay");
                    boolean autoplayDisabled = autoplay == null;

                    String tabindex = video.getAttribute("tabindex");
                    boolean keyboardAccessible = !"-1".equals(tabindex);

                    boolean compliant = autoplayDisabled;

                    Map<String, Object> item = new HashMap<>();
                    item.put("video_url", videoUrl != null ? videoUrl : "");
                    item.put("element_html", videoHtml != null ? videoHtml : "");
                    item.put("autoplay_disabled", autoplayDisabled);
                    item.put("keyboard_accessible", keyboardAccessible);
                    item.put("compliant", compliant);
                    item.put("message", compliant ? "자동 재생 비활성화됨" : "자동 재생 활성화 - 비활성화 필요");
                    results.add(item);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("checkVideoAutoplay failed", e);
            results.add(Map.of("error", "비디오 자동재생 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 11. Contrast (3번)
    // -----------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkContrast() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            // Find elements with text using locator
            List<Locator> elements = page.locator("body *:not(script):not(style)").all();

            for (Locator element : elements) {
                try {
                    String text = null;
                    try { text = element.innerText(); } catch (Exception ignored) {}
                    if (text == null || text.isBlank()) {
                        try { text = element.textContent(); } catch (Exception ignored) {}
                    }
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    text = text.trim();
                    if (text.length() > 200 || !text.matches(".*[a-zA-Z0-9가-힣].*")) {
                        continue;
                    }

                    String elementHtml = "";
                    try { elementHtml = (String) element.evaluate("el => el.outerHTML"); } catch (Exception ignored) {}

                    Map<String, Object> styles = null;
                    ElementHandle handle = element.elementHandle();
                    if (handle != null) {
                        try {
                            styles = (Map<String, Object>) page.evaluate(scripts.get("contrast_styles"), handle);
                        } catch (Exception ignored) {}
                    }
                    if (styles == null) {
                        styles = Map.of("color", "rgb(0, 0, 0)", "backgroundColor", "rgb(255, 255, 255)");
                    }

                    String fgColor = (String) styles.getOrDefault("color", "rgb(0, 0, 0)");
                    String bgColor = (String) styles.getOrDefault("backgroundColor", "rgb(255, 255, 255)");

                    double contrastRatio = ContrastCalculator.calculateContrastRatio(fgColor, bgColor);
                    boolean compliant = contrastRatio >= 4.5;

                    Map<String, Object> item = new HashMap<>();
                    item.put("text", text.length() > 50 ? text.substring(0, 50) + "..." : text);
                    item.put("element_html", elementHtml);
                    item.put("foreground_color", fgColor);
                    item.put("background_color", bgColor);
                    item.put("contrast_ratio", Math.round(contrastRatio * 100.0) / 100.0);
                    item.put("wcag_compliant", compliant);
                    results.add(item);

                    if (results.size() >= 100) {
                        break;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.error("checkContrast failed", e);
            results.add(Map.of("error", "색상 대비 검사 실패: " + e.getMessage()));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 12. Pause Control (15번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkPauseControl() {
        return checkPauseControl(5); // Default 5 seconds for fast accurate check
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> checkPauseControl(int durationSeconds) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            // 1. Scroll page to trigger lazy loading / animations
            try {
                page.setViewportSize(1920, 1080);
                page.evaluate(scripts.get("pause_control_scroll"));
                page.evaluate("window.scrollTo(0, 0);");
                page.waitForTimeout(1000);
            } catch (Exception ignored) {}

            // 2. Run auditor
            Map<String, Object> autoContentInfo = (Map<String, Object>) page.evaluate(
                    scripts.get("pause_control_auditor"), durationSeconds);

            if (autoContentInfo != null) {
                List<Map<String, Object>> autoElements = (List<Map<String, Object>>) autoContentInfo.get("autoElements");
                if (autoElements != null) {
                    for (Map<String, Object> el : autoElements) {
                        boolean hasAutoplay = Boolean.TRUE.equals(el.get("hasAutoplay"));
                        boolean hasPause = Boolean.TRUE.equals(el.get("hasPauseButton"));
                        boolean compliant = !hasAutoplay || hasPause;

                        Map<String, Object> item = new HashMap<>();
                        item.put("tag", el.get("tag"));
                        item.put("element_html", el.get("html"));
                        item.put("has_autoplay", hasAutoplay);
                        item.put("has_pause", hasPause);
                        item.put("compliant", compliant);
                        item.put("message", compliant ? "정지 기능 제공됨" : "자동 이동 콘텐츠에 정지 기능 누락");
                        results.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("checkPauseControl failed", e);
            results.add(Map.of("error", "정지 기능 검사 실패: " + e.getMessage(), "compliant", false));
        }
        return results;
    }

    // -----------------------------------------------------------------------------------------
    // 13. Alt Text (1번)
    // -----------------------------------------------------------------------------------------
    public List<Map<String, Object>> checkAltText() {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            List<Locator> images = page.locator("img").all();

            for (int idx = 0; idx < images.size(); idx++) {
                Locator img = images.get(idx);
                String imgUrl = null;
                String altText = null;
                String elementHtml = "";

                try {
                    imgUrl = img.getAttribute("src");
                    altText = img.getAttribute("alt");
                    elementHtml = (String) img.evaluate("el => el.outerHTML");
                } catch (Exception ignored) {}

                if (imgUrl == null || imgUrl.isBlank() || "N/A".equalsIgnoreCase(imgUrl)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("img_url", "N/A");
                    item.put("element_html", elementHtml != null ? elementHtml : "");
                    item.put("alt_text", altText != null ? altText : "N/A");
                    item.put("answer", "src 속성이 없는 이미지");
                    item.put("compliant", 3);
                    results.add(item);
                    continue;
                }

                // SVG 이미지인 경우
                if (isSvgImage(imgUrl)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("img_url", imgUrl);
                    item.put("element_html", elementHtml != null ? elementHtml : "");
                    item.put("alt_text", altText != null ? altText : "N/A");
                    item.put("answer", "SVG 이미지는 검사 대상에서 제외됩니다");
                    item.put("compliant", 0);
                    item.put("ai_type", "장식적 이미지");
                    item.put("ai_grade", "준수(매우높음)");
                    item.put("ai_reason", "SVG 이미지는 접근성 검사 대상에서 제외");
                    item.put("ai_improvement", "검사 제외");
                    item.put("has_error", false);
                    results.add(item);
                    continue;
                }

                // 이미지 처리
                boolean hasAlt = altText != null && !altText.trim().isEmpty() && !"N/A".equalsIgnoreCase(altText);

                if (!hasAlt) {
                    if (aiClient != null && aiClient.isEnabled()) {
                        String preparedImg = prepareImageForAi(imgUrl);
                        AltTextAiResult aiResult = aiClient.generateAltText(preparedImg);
                        if (aiResult != null) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("img_url", imgUrl);
                            item.put("element_html", elementHtml);
                            item.put("alt_text", "N/A");
                            item.put("answer", aiResult.improvement());
                            item.put("compliant", aiResult.hasError() ? 3 : 2);
                            item.put("ai_type", aiResult.type());
                            item.put("ai_grade", aiResult.grade());
                            item.put("ai_reason", aiResult.reason());
                            item.put("ai_improvement", aiResult.improvement());
                            item.put("has_error", aiResult.hasError());
                            results.add(item);
                            continue;
                        }
                    }
                    // Fallback when AI is not configured or fails
                    Map<String, Object> item = new HashMap<>();
                    item.put("img_url", imgUrl);
                    item.put("element_html", elementHtml);
                    item.put("alt_text", "N/A");
                    item.put("answer", "AI 분석 불가 - 대체 텍스트가 필요합니다");
                    item.put("compliant", 2);
                    item.put("ai_type", "알 수 없음");
                    item.put("ai_grade", "매우낮음");
                    item.put("ai_reason", "AI 분석 기능 비활성화");
                    item.put("ai_improvement", "AI 분석 기능을 사용할 수 없습니다");
                    item.put("has_error", true);
                    results.add(item);
                } else {
                    if (aiClient != null && aiClient.isEnabled()) {
                        String preparedImg = prepareImageForAi(imgUrl);
                        AltTextAiResult aiResult = aiClient.evaluateAltText(preparedImg, altText);
                        if (aiResult != null) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("img_url", imgUrl);
                            item.put("element_html", elementHtml);
                            item.put("alt_text", altText);
                            item.put("answer", aiResult.improvement());
                            item.put("compliant", aiResult.compliant());
                            item.put("ai_type", aiResult.type());
                            item.put("ai_grade", aiResult.grade());
                            item.put("ai_reason", aiResult.reason());
                            item.put("ai_improvement", aiResult.improvement());
                            item.put("has_error", aiResult.hasError());
                            results.add(item);
                            continue;
                        }
                    }
                    // Fallback when AI is not configured: check non-empty alt text
                    int compliant = altText.trim().isEmpty() ? 2 : 0;
                    Map<String, Object> item = new HashMap<>();
                    item.put("img_url", imgUrl);
                    item.put("element_html", elementHtml);
                    item.put("alt_text", altText);
                    item.put("answer", compliant == 0 ? "문제 없음" : "alt를 기반으로 한 설명이 필요합니다");
                    item.put("compliant", compliant);
                    item.put("ai_type", "알 수 없음");
                    item.put("ai_grade", compliant == 0 ? "매우높음" : "매우낮음");
                    item.put("ai_reason", "AI 분석 기능 비활성화");
                    item.put("ai_improvement", compliant == 0 ? altText : "대체텍스트 작성 필요");
                    item.put("has_error", false);
                    results.add(item);
                }
            }
        } catch (Exception e) {
            log.error("checkAltText failed", e);
            results.add(Map.of(
                    "img_url", "N/A",
                    "element_html", "",
                    "alt_text", "N/A",
                    "answer", "페이지 검사 실패: " + e.getMessage(),
                    "compliant", 3
            ));
        }
        return results;
    }

    private boolean isSvgImage(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank() || "N/A".equalsIgnoreCase(imgUrl)) {
            return false;
        }
        return imgUrl.startsWith("data:image/svg+xml") || imgUrl.toLowerCase().endsWith(".svg");
    }

    private String prepareImageForAi(String imgUrl) {
        try {
            if (imgUrl.startsWith("http://") || imgUrl.startsWith("https://") || imgUrl.startsWith("data:")) {
                return imgUrl;
            }
            URI base = URI.create(baseUrl);
            return base.resolve(imgUrl).toString();
        } catch (Exception e) {
            // Attempt screenshot capture as fallback
            try {
                Locator locator = page.locator("img[src=\"" + imgUrl + "\"]").first();
                if (locator.count() > 0 && locator.isVisible()) {
                    byte[] screenshot = locator.screenshot();
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(screenshot);
                }
            } catch (Exception ignored) {}
            return imgUrl;
        }
    }

    @Override
    public void close() {
        if (page != null) {
            try { page.close(); } catch (Exception ignored) {}
            page = null;
        }
        if (context != null) {
            try { context.close(); } catch (Exception ignored) {}
            context = null;
        }
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
            browser = null;
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
            playwright = null;
        }
    }
}
