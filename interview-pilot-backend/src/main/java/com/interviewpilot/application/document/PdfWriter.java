package com.interviewpilot.application.document;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Small text-only PDF writer on top of PDFBox: A4, one column, word wrap and page breaks. The output is real
 * selectable text with standard fonts, which keeps resumes ATS-friendly. Characters the standard fonts cannot
 * draw are replaced with close ASCII equivalents (or "?") instead of failing.
 */
public final class PdfWriter implements AutoCloseable {

    private static final float MARGIN = 54;
    private static final float WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final Color TEXT = new Color(0x1f, 0x29, 0x37);
    private static final Color MUTED = new Color(0x55, 0x60, 0x6e);
    private static final Color RULE = new Color(0xc9, 0xcf, 0xd8);

    private final PDDocument document = new PDDocument();
    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final Map<Integer, Boolean> drawable = new HashMap<>();
    private PDPageContentStream stream;
    private float y;

    public PdfWriter(String title, String author) {
        document.getDocumentInformation().setTitle(clean(title));
        document.getDocumentInformation().setAuthor(clean(author));
        newPage();
    }

    public void heading(String text, float size) {
        write(text, bold, size, TEXT, 0);
    }

    public void text(String text, float size) {
        write(text, regular, size, TEXT, 0);
    }

    public void boldText(String text, float size) {
        write(text, bold, size, TEXT, 0);
    }

    public void muted(String text, float size) {
        write(text, regular, size, MUTED, 0);
    }

    public void bullet(String text, float size) {
        float lineHeight = size * 1.35f;
        ensureSpace(lineHeight);
        draw("•", regular, size, TEXT, MARGIN + 4, y - size);
        write(text, regular, size, TEXT, 16);
    }

    /** Upper-case section title with a thin rule underneath. */
    public void section(String title) {
        space(10);
        ensureSpace(30);
        write(title.toUpperCase(), bold, 10.5f, TEXT, 0);
        try {
            stream.setStrokingColor(RULE);
            stream.setLineWidth(0.6f);
            stream.moveTo(MARGIN, y - 2);
            stream.lineTo(MARGIN + WIDTH, y - 2);
            stream.stroke();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        space(8);
    }

    public void space(float points) {
        y -= points;
    }

    public byte[] toBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            stream.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public void close() {
        try {
            document.close();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Word-wraps {@code text} at the current position; blank text writes nothing. */
    private void write(String text, PDType1Font font, float size, Color color, float indent) {
        String safe = clean(text);
        if (safe.isBlank()) return;
        float lineHeight = size * 1.35f;
        for (String line : wrap(safe, font, size, WIDTH - indent)) {
            ensureSpace(lineHeight);
            draw(line, font, size, color, MARGIN + indent, y - size);
            y -= lineHeight;
        }
    }

    private List<String> wrap(String text, PDType1Font font, float size, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (width(candidate, font, size) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private float width(String text, PDType1Font font, float size) {
        try {
            return font.getStringWidth(text) / 1000 * size;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void draw(String text, PDType1Font font, float size, Color color, float x, float baseline) {
        try {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(text);
            stream.endText();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void ensureSpace(float needed) {
        if (y - needed < MARGIN) newPage();
    }

    private void newPage() {
        try {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Normalises whitespace and typography, and replaces characters the standard fonts cannot draw. */
    private String clean(String text) {
        if (text == null) return "";
        String normalised = text
                .replaceAll("[\\u2018\\u2019\\u201A\\u2032]", "'")
                .replaceAll("[\\u201C\\u201D\\u201E\\u2033]", "\"")
                .replaceAll("[\\u2013\\u2014\\u2212]", "-")
                .replace("…", "...")
                .replaceAll("[\\u00A0\\t\\r\\n]+", " ")
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll(" {2,}", " ")
                .trim();
        StringBuilder out = new StringBuilder(normalised.length());
        normalised.codePoints().forEach(codePoint -> out.append(canDraw(codePoint) ? Character.toString(codePoint) : "?"));
        return out.toString();
    }

    private boolean canDraw(int codePoint) {
        return drawable.computeIfAbsent(codePoint, point -> {
            try {
                regular.encode(Character.toString(point));
                return true;
            } catch (IllegalArgumentException | IOException exception) {
                return false;
            }
        });
    }
}
