package com.secondbrain.service.impl;

import com.secondbrain.entity.RawChatRecord;
import com.secondbrain.kafka.KafkaProducerService;
import com.secondbrain.service.DocumentCaptureService;
import com.secondbrain.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 文档采集服务实现类.
 * <p>解析上传的文件内容并发送到Kafka进行后续处理</p>
 */
@Service
public class DocumentCaptureServiceImpl implements DocumentCaptureService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCaptureServiceImpl.class);

    private final FileProcessingService fileProcessingService;
    private final KafkaProducerService kafkaProducerService;

    public DocumentCaptureServiceImpl(FileProcessingService fileProcessingService, KafkaProducerService kafkaProducerService) {
        this.fileProcessingService = fileProcessingService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public String extractContent(MultipartFile file) {
        log.info("开始提取文档内容，文件名：{}", file.getOriginalFilename());
        String content = fileProcessingService.processFile(file);
        log.info("文档内容提取成功，文件名：{}，内容长度：{}", file.getOriginalFilename(), content.length());
        return content;
    }

    @Override
    public void sendToCapture(String content, Long userId, String source) {
        log.info("发送内容到采集处理，userId：{}，source：{}，内容长度：{}", userId, source, content.length());

        RawChatRecord record = new RawChatRecord();
        record.setUserId(userId);
        record.setContent(content);
        record.setSourceUrl(source);
        record.setCreateTime(LocalDateTime.now());

        kafkaProducerService.sendChatCollect(record);
        log.info("内容发送到采集处理成功");
    }
}
