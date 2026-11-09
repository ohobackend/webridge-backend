package com.webridge.scan.report;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

@Slf4j
public class PdfFontHelper {

    private static BaseFont regularBase;
    private static BaseFont boldBase;

    static {
        try {
            byte[] regBytes = loadFontBytes("/fonts/NanumGothic.ttf");
            if (regBytes != null) {
                regularBase = BaseFont.createFont("NanumGothic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, regBytes, null);
            }
            byte[] boldBytes = loadFontBytes("/fonts/NanumGothic-Bold.ttf");
            if (boldBytes != null) {
                boldBase = BaseFont.createFont("NanumGothic-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, boldBytes, null);
            } else {
                boldBase = regularBase;
            }
        } catch (Exception e) {
            log.warn("Failed to load NanumGothic embedded font, falling back to default Helvetica", e);
        }
    }

    private static byte[] loadFontBytes(String path) {
        try (InputStream is = PdfFontHelper.class.getResourceAsStream(path)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.warn("Could not read font from classpath: {}", path, e);
        }
        return null;
    }

    public static Font regular(float size) {
        if (regularBase != null) {
            return new Font(regularBase, size, Font.NORMAL);
        }
        return new Font(Font.HELVETICA, size, Font.NORMAL);
    }

    public static Font bold(float size) {
        if (boldBase != null) {
            return new Font(boldBase, size, Font.BOLD);
        } else if (regularBase != null) {
            return new Font(regularBase, size, Font.BOLD);
        }
        return new Font(Font.HELVETICA, size, Font.BOLD);
    }
}
