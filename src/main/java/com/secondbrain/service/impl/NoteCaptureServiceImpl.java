package com.secondbrain.service.impl;

import com.secondbrain.dto.NoteCaptureRequest;
import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.service.NoteCaptureService;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NoteCaptureServiceImpl implements NoteCaptureService {

    private static final Logger log = LoggerFactory.getLogger(NoteCaptureServiceImpl.class);

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Override
    public String captureMarkdownNote(NoteCaptureRequest request) {
        log.info("【NoteCaptureService】开始捕捉笔记，标题：{}，用户 ID：{}", request.getTitle(), request.getUserId());
        
        try {
            String content = parseMarkdown(request.getContent());
            log.info("【NoteCaptureService】Markdown 解析成功，原始长度：{}，解析后长度：{}", 
                request.getContent().length(), content.length());

            RawChatRecord record = new RawChatRecord();
            record.setUserId(request.getUserId());
            record.setContent(content);
            record.setSourceUrl("NOTE:" + request.getTitle());
            record.setCreateTime(LocalDateTime.now());
            
            log.info("【NoteCaptureService】准备发送 Kafka 消息，record: userId={}, contentLength={}, sourceUrl={}", 
                record.getUserId(), record.getContent().length(), record.getSourceUrl());

            kafkaProducerService.sendChatCollect(record);
            
            log.info("【NoteCaptureService】Kafka 消息发送成功，标题：{}", request.getTitle());
            log.info("笔记捕捉成功，标题：{}，内容长度：{}", request.getTitle(), content.length());
            return "笔记捕捉成功";
        } catch (Exception e) {
            log.error("【NoteCaptureService】笔记捕捉失败，标题：{}", request.getTitle(), e);
            throw new RuntimeException("笔记捕捉失败：" + e.getMessage());
        }
    }

    private String parseMarkdown(String markdown) {
        try {
            Parser parser = Parser.builder().build();
            Document document = parser.parse(markdown);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String html = renderer.render(document);

            String text = Jsoup.parse(html).text();
            text = text.replaceAll("\\s+", " ").trim();

            log.info("Markdown解析成功，原始长度：{}，解析后长度：{}", markdown.length(), text.length());
            return text;
        } catch (Exception e) {
            log.error("Markdown解析失败", e);
            throw new RuntimeException("Markdown解析失败：" + e.getMessage());
        }
    }
}
