package com.secondbrain.service.impl;

import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.service.DocumentCaptureService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentCaptureServiceImpl implements DocumentCaptureService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCaptureServiceImpl.class);

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    @Override
    public String extractContent(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String lowerFilename = filename.toLowerCase();

        if (lowerFilename.endsWith(".pdf")) {
            return extractFromPDF(file);
        } else if (lowerFilename.endsWith(".docx")) {
            return extractFromDocx(file);
        } else if (lowerFilename.endsWith(".doc")) {
            return extractFromDoc(file);
        } else if (lowerFilename.endsWith(".txt")) {
            return extractFromTxt(file);
        } else if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xls")) {
            return extractFromExcel(file);
        } else {
            throw new UnsupportedOperationException("不支持的文件格式：" + filename);
        }
    }

    private String extractFromPDF(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        log.info("PDF文件提取成功，文件名：{}，内容长度：{}", file.getOriginalFilename(), text.length());
        return text;
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        XWPFDocument document = new XWPFDocument(file.getInputStream());
        StringBuilder text = new StringBuilder();
        
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph paragraph : paragraphs) {
            text.append(paragraph.getText()).append("\n");
        }
        
        document.close();
        log.info("DOCX文件提取成功，文件名：{}，内容长度：{}", file.getOriginalFilename(), text.length());
        return text.toString();
    }

    private String extractFromDoc(MultipartFile file) throws IOException {
        HWPFDocument document = new HWPFDocument(file.getInputStream());
        WordExtractor extractor = new WordExtractor(document);
        String text = extractor.getText();
        document.close();
        log.info("DOC文件提取成功，文件名：{}，内容长度：{}", file.getOriginalFilename(), text.length());
        return text;
    }

    private String extractFromTxt(MultipartFile file) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        log.info("TXT文件提取成功，文件名：{}，内容长度：{}", file.getOriginalFilename(), text.length());
        return text;
    }

    private String extractFromExcel(MultipartFile file) throws IOException {
        org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream());
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
            content.append("Sheet ").append(i + 1).append(":\n");

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.isEmpty()) {
                        content.append(cellValue).append("\t");
                    }
                }
                content.append("\n");
            }
        }

        workbook.close();
        log.info("Excel文件提取成功，文件名：{}，内容长度：{}", file.getOriginalFilename(), content.length());
        return content.toString();
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    @Override
    public void sendToCapture(String content, Long userId, String source) {
        try {
            RawChatRecord record = new RawChatRecord();
            record.setUserId(userId);
            record.setContent(content);
            record.setSourceUrl("DOCUMENT:" + source);
            record.setCreateTime(LocalDateTime.now());

            if (kafkaProducerService == null) {
                log.warn("Kafka未启用，跳过文档内容发送，用户ID：{}，来源：{}", userId, source);
                return;
            }
            
            kafkaProducerService.sendChatCollect(record);
            log.info("文档内容已发送到捕捉层，用户ID：{}，来源：{}", userId, source);
        } catch (Exception e) {
            log.error("发送文档内容到捕捉层失败", e);
            throw new RuntimeException("发送失败", e);
        }
    }
}
