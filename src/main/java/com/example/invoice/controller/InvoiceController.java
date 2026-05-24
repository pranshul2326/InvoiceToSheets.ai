package com.example.invoice.controller;

import com.example.invoice.dto.InvoiceResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final ChatClient chatClient;

    public InvoiceController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
            .defaultSystem("You are an expert B2B visual extraction engine. Analyze the provided image or text invoice, transcribe any messy handwriting, and extract the layout data exactly matching the requested schema fields. Set missing parameters to null.")
            .build();
    }

    @PostMapping("/parse-invoice")
    public ResponseEntity<InvoiceResponse> parseInvoiceText(@RequestBody Map<String, String> payload) {
        String rawText = payload.get("rawText");
        if (rawText == null || rawText.isBlank()) return ResponseEntity.badRequest().build();

        InvoiceResponse result;
        if (rawText.startsWith("data:image/")) {
            try {
                String[] parts = rawText.split(",");
                String mimeType = parts[0].split(";")[0].replace("data:", "");
                byte[] imageBytes = Base64.getDecoder().decode(parts[1]);
                result = chatClient.prompt()
                    .user(u -> u.text("Transcribe and extract data fields from this invoice document image.")
                        .media(new Media(MimeTypeUtils.parseMimeType(mimeType), new ByteArrayResource(imageBytes))))
                    .call()
                    .entity(InvoiceResponse.class);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        } else {
            result = extract(rawText);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/parse-invoice", consumes = "multipart/form-data")
    public ResponseEntity<InvoiceResponse> parseInvoiceFile(@RequestParam("file") MultipartFile file) throws Exception {
        String rawText;
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                rawText = new PDFTextStripper().getText(doc);
            }
        } else {
            rawText = new String(file.getBytes());
        }
        if (rawText.isBlank()) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(extract(rawText));
    }

    private InvoiceResponse extract(String rawText) {
        return chatClient.prompt()
            .user(u -> u.text("Extract parameters from this document text:\n\n{input}").param("input", rawText))
            .call()
            .entity(InvoiceResponse.class);
    }
}
