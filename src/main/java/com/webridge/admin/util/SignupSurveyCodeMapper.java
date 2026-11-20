package com.webridge.admin.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SignupSurveyCodeMapper {

    private static final Map<String, String> PATH_MAP = new LinkedHashMap<>();
    private static final Map<String, String> OCCUPATION_MAP = new LinkedHashMap<>();

    static {
        PATH_MAP.put("a", "SNS");
        PATH_MAP.put("b", "웹 포털 검색");
        PATH_MAP.put("c", "동료 / 지인 추천");
        PATH_MAP.put("d", "커뮤니티 (슬랙 / 오픈채팅방)");
        PATH_MAP.put("e", "이메일 홍보");
        PATH_MAP.put("f", "기타");

        OCCUPATION_MAP.put("a", "PM/PO");
        OCCUPATION_MAP.put("b", "기획");
        OCCUPATION_MAP.put("c", "디자인");
        OCCUPATION_MAP.put("d", "개발 / 엔지니어링");
        OCCUPATION_MAP.put("e", "웹 접근성 전문가 / 컨설턴트");
        OCCUPATION_MAP.put("f", "경영 / 의사결정권자");
        OCCUPATION_MAP.put("g", "기타");
    }

    private SignupSurveyCodeMapper() {}

    public static String getPathLabel(String code, String other) {
        if (code == null || code.isBlank()) {
            return "미응답";
        }
        String label = PATH_MAP.getOrDefault(code.toLowerCase(), "알 수 없음 (" + code + ")");
        if ("f".equalsIgnoreCase(code) && other != null && !other.isBlank()) {
            return label + " (" + other + ")";
        }
        return label;
    }

    public static String getOccupationLabel(String code, String other) {
        if (code == null || code.isBlank()) {
            return "미응답";
        }
        String label = OCCUPATION_MAP.getOrDefault(code.toLowerCase(), "알 수 없음 (" + code + ")");
        if ("g".equalsIgnoreCase(code) && other != null && !other.isBlank()) {
            return label + " (" + other + ")";
        }
        return label;
    }

    public static Map<String, String> getAllPathChoices() {
        return new LinkedHashMap<>(PATH_MAP);
    }

    public static Map<String, String> getAllOccupationChoices() {
        return new LinkedHashMap<>(OCCUPATION_MAP);
    }
}
