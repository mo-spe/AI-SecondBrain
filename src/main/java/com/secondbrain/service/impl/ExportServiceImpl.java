package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.RawChatRecordMapper;
import com.secondbrain.service.ExportService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final RawChatRecordMapper rawChatRecordMapper;

    public ExportServiceImpl(KnowledgeNodeMapper knowledgeNodeMapper, RawChatRecordMapper rawChatRecordMapper) {
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.rawChatRecordMapper = rawChatRecordMapper;
    }

    @Override
    public void exportToMarkdown(Long userId, List<Long> ids, HttpServletResponse response) {
        try {
            LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getUserId, userId)
                    .orderByDesc(KnowledgeNode::getCreateTime);
            
            if (ids != null && !ids.isEmpty()) {
                wrapper.in(KnowledgeNode::getId, ids);
            }
            
            List<KnowledgeNode> knowledgeList = knowledgeNodeMapper.selectList(wrapper);

            StringBuilder markdown = new StringBuilder();
            markdown.append("# AI第二大脑 - 知识点导出\n\n");
            markdown.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            markdown.append("---\n\n");

            for (KnowledgeNode knowledge : knowledgeList) {
                markdown.append("## ").append(knowledge.getTitle()).append("\n\n");
                markdown.append("**摘要**: ").append(knowledge.getSummary()).append("\n\n");
                markdown.append("**内容**:\n\n").append(knowledge.getContentMd()).append("\n\n");
                markdown.append("**重要程度**: ").append(getImportanceText(knowledge.getImportance())).append("\n\n");
                markdown.append("**掌握程度**: ").append(getMasteryText(knowledge.getMasteryLevel())).append("\n\n");
                markdown.append("**创建时间**: ").append(formatDateTime(knowledge.getCreateTime())).append("\n\n");
                markdown.append("---\n\n");
            }

            response.setContentType("text/markdown; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=knowledge_export.md");
            response.setCharacterEncoding("UTF-8");

            try (OutputStream out = response.getOutputStream()) {
                out.write(markdown.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            log.info("Markdown导出成功，userId: {}, 知识点数量: {}", userId, knowledgeList.size());
        } catch (IOException e) {
            log.error("Markdown导出失败，userId: {}", userId, e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public void exportToPDF(Long userId, List<Long> ids, HttpServletResponse response) {
        try {
            LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getUserId, userId)
                    .orderByDesc(KnowledgeNode::getCreateTime);
            
            if (ids != null && !ids.isEmpty()) {
                wrapper.in(KnowledgeNode::getId, ids);
            }
            
            List<KnowledgeNode> knowledgeList = knowledgeNodeMapper.selectList(wrapper);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            document.add(new Paragraph("AI Second Brain - Knowledge Export")
                .setFont(font)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Export Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFont(font)
                .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            for (KnowledgeNode knowledge : knowledgeList) {
                document.add(new Paragraph("Title: " + knowledge.getTitle())
                    .setFont(font)
                    .setFontSize(16)
                    .setBold());

                document.add(new Paragraph("Summary: " + knowledge.getSummary())
                    .setFont(font));

                document.add(new Paragraph("Content:\n" + knowledge.getContentMd())
                    .setFont(font));

                document.add(new Paragraph("Importance: " + getImportanceText(knowledge.getImportance()))
                    .setFont(font));

                document.add(new Paragraph("Mastery: " + getMasteryText(knowledge.getMasteryLevel()))
                    .setFont(font));

                document.add(new Paragraph("Create Time: " + formatDateTime(knowledge.getCreateTime()))
                    .setFont(font));

                document.add(new Paragraph("\n-------------------\n")
                    .setFont(font));
            }

            document.close();

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=knowledge_export.pdf");

            try (OutputStream out = response.getOutputStream()) {
                out.write(baos.toByteArray());
                out.flush();
            }

            log.info("PDF导出成功，userId: {}, 知识点数量: {}", userId, knowledgeList.size());
        } catch (Exception e) {
            log.error("PDF导出失败，userId: {}", userId, e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public void exportToWord(Long userId, List<Long> ids, HttpServletResponse response) {
        try {
            LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getUserId, userId)
                    .orderByDesc(KnowledgeNode::getCreateTime);
            
            if (ids != null && !ids.isEmpty()) {
                wrapper.in(KnowledgeNode::getId, ids);
            }
            
            List<KnowledgeNode> knowledgeList = knowledgeNodeMapper.selectList(wrapper);

            XWPFDocument document = new XWPFDocument();

            XWPFParagraph titlePara = document.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("AI第二大脑 - 知识点导出");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            XWPFParagraph timePara = document.createParagraph();
            XWPFRun timeRun = timePara.createRun();
            timeRun.setText("导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            for (KnowledgeNode knowledge : knowledgeList) {
                XWPFParagraph headingPara = document.createParagraph();
                XWPFRun headingRun = headingPara.createRun();
                headingRun.setText(knowledge.getTitle());
                headingRun.setBold(true);
                headingRun.setFontSize(16);

                addParagraph(document, "摘要: " + knowledge.getSummary());
                addParagraph(document, "内容:\n" + knowledge.getContentMd());
                addParagraph(document, "重要程度: " + getImportanceText(knowledge.getImportance()));
                addParagraph(document, "掌握程度: " + getMasteryText(knowledge.getMasteryLevel()));
                addParagraph(document, "创建时间: " + formatDateTime(knowledge.getCreateTime()));
                addParagraph(document, "-------------------");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            document.close();

            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition", "attachment; filename=knowledge_export.docx");

            try (OutputStream out = response.getOutputStream()) {
                out.write(baos.toByteArray());
                out.flush();
            }

            log.info("Word导出成功，userId: {}, 知识点数量: {}", userId, knowledgeList.size());
        } catch (IOException e) {
            log.error("Word导出失败，userId: {}", userId, e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public void exportToJSON(Long userId, List<Long> ids, HttpServletResponse response) {
        try {
            LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getUserId, userId)
                    .orderByDesc(KnowledgeNode::getCreateTime);
            
            if (ids != null && !ids.isEmpty()) {
                wrapper.in(KnowledgeNode::getId, ids);
            }
            
            List<KnowledgeNode> knowledgeList = knowledgeNodeMapper.selectList(wrapper);

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"exportTime\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            json.append("  \"userId\": ").append(userId).append(",\n");
            json.append("  \"knowledgeCount\": ").append(knowledgeList.size()).append(",\n");
            json.append("  \"knowledgeList\": [\n");

            for (int i = 0; i < knowledgeList.size(); i++) {
                KnowledgeNode knowledge = knowledgeList.get(i);
                json.append("    {\n");
                json.append("      \"id\": ").append(knowledge.getId()).append(",\n");
                json.append("      \"title\": \"").append(escapeJson(knowledge.getTitle())).append("\",\n");
                json.append("      \"summary\": \"").append(escapeJson(knowledge.getSummary())).append("\",\n");
                json.append("      \"content\": \"").append(escapeJson(knowledge.getContentMd())).append("\",\n");
                json.append("      \"importance\": ").append(knowledge.getImportance()).append(",\n");
                json.append("      \"masteryLevel\": ").append(knowledge.getMasteryLevel()).append(",\n");
                json.append("      \"createTime\": \"").append(formatDateTime(knowledge.getCreateTime())).append("\"\n");
                json.append("    }");
                if (i < knowledgeList.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            response.setContentType("application/json; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=knowledge_export.json");
            response.setCharacterEncoding("UTF-8");

            try (OutputStream out = response.getOutputStream()) {
                out.write(json.toString().getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            log.info("JSON导出成功，userId: {}, 知识点数量: {}", userId, knowledgeList.size());
        } catch (IOException e) {
            log.error("JSON导出失败，userId: {}", userId, e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public void exportToCSV(Long userId, List<Long> ids, HttpServletResponse response) {
        try {
            LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<KnowledgeNode>()
                    .eq(KnowledgeNode::getUserId, userId)
                    .orderByDesc(KnowledgeNode::getCreateTime);
            
            if (ids != null && !ids.isEmpty()) {
                wrapper.in(KnowledgeNode::getId, ids);
            }
            
            List<KnowledgeNode> knowledgeList = knowledgeNodeMapper.selectList(wrapper);

            StringBuilder csv = new StringBuilder();
            csv.append("ID,标题,摘要,内容,重要程度,掌握程度,创建时间\n");

            for (KnowledgeNode knowledge : knowledgeList) {
                csv.append(knowledge.getId()).append(",");
                csv.append(escapeCSV(knowledge.getTitle())).append(",");
                csv.append(escapeCSV(knowledge.getSummary())).append(",");
                csv.append(escapeCSV(knowledge.getContentMd())).append(",");
                csv.append(getImportanceText(knowledge.getImportance())).append(",");
                csv.append(getMasteryText(knowledge.getMasteryLevel())).append(",");
                csv.append(formatDateTime(knowledge.getCreateTime())).append("\n");
            }

            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=knowledge_export.csv");
            response.setCharacterEncoding("UTF-8");

            try (OutputStream out = response.getOutputStream()) {
                out.write(("\uFEFF" + csv.toString()).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            log.info("CSV导出成功，userId: {}, 知识点数量: {}", userId, knowledgeList.size());
        } catch (IOException e) {
            log.error("CSV导出失败，userId: {}", userId, e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setText(text);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String getImportanceText(Integer importance) {
        if (importance == null) return "未知";
        return switch (importance) {
            case 1 -> "低";
            case 2 -> "中";
            case 3 -> "高";
            default -> "未知";
        };
    }

    private String getMasteryText(Integer mastery) {
        if (mastery == null) return "未知";
        return switch (mastery) {
            case 1 -> "未掌握";
            case 2 -> "初步掌握";
            case 3 -> "基本掌握";
            case 4 -> "熟练掌握";
            case 5 -> "精通";
            default -> "未知";
        };
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String escapeCSV(String text) {
        if (text == null) return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
