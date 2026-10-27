package com.salesnav360.pdf;

import com.salesnav360.dto.QuotationLineItemResponse;
import com.salesnav360.dto.QuotationResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class QuotationPdfService {

    private static final float LEFT_MARGIN = 50;
    private static final float TOP = 750;
    private static final float LINE_HEIGHT = 18;
    private static final int LINES_PER_PAGE = 38;

    public byte[] generate(QuotationResponse quotation) {
        List<String> lines = createLines(quotation);
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int start = 0; start < lines.size(); start += LINES_PER_PAGE) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.setFont(font, 11);
                    int end = Math.min(start + LINES_PER_PAGE, lines.size());
                    for (int index = start; index < end; index++) {
                        content.beginText();
                        content.newLineAtOffset(LEFT_MARGIN, TOP - ((index - start) * LINE_HEIGHT));
                        content.showText(toPdfText(lines.get(index)));
                        content.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate the quotation PDF.", exception);
        }
    }

    private static List<String> createLines(QuotationResponse quotation) {
        List<String> lines = new ArrayList<>();
        lines.add("SalesNav360 - Quotation");
        lines.add("Quotation number: " + quotation.quotationNumber());
        lines.add("Client: " + quotation.clientName());
        lines.add("Status: " + quotation.status());
        lines.add("Valid until: " + quotation.validUntil());
        lines.add("");
        lines.add("Items");
        for (QuotationLineItemResponse item : quotation.lineItems()) {
            String description = item.description() == null ? "" : item.description();
            lines.addAll(wrap(description, 36));
            lines.add("  Quantity: " + item.quantity()
                    + " | Unit price: " + item.unitPrice()
                    + " | Line total: " + item.lineTotal());
        }
        lines.add("");
        lines.add("Subtotal: " + quotation.subtotal());
        lines.add("Discount: " + quotation.discount() + " (" + quotation.discountPercent() + "%)");
        lines.add("Tax: " + quotation.tax() + " (" + quotation.taxPercent() + "%)");
        lines.add("Grand total: " + quotation.grandTotal());
        return lines;
    }

    private static List<String> wrap(String value, int width) {
        List<String> wrapped = new ArrayList<>();
        String remaining = value;
        while (remaining.length() > width) {
            int breakAt = remaining.lastIndexOf(' ', width);
            if (breakAt <= 0) {
                breakAt = width;
            }
            wrapped.add(remaining.substring(0, breakAt));
            remaining = remaining.substring(breakAt).stripLeading();
        }
        wrapped.add(remaining);
        return wrapped;
    }

    private static String toPdfText(String value) {
        StringBuilder text = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            text.append(character >= 32 && character <= 126 ? character : '?');
        }
        return text.toString();
    }
}
