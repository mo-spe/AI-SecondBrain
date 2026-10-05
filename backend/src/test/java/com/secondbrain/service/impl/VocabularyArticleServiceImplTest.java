package com.secondbrain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.VocabularyArticleDtos;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.VocabularyArticleMapper;
import com.secondbrain.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证文章覆盖结论独立于模型自报内容，并避免无效词表产生费用。 */
class VocabularyArticleServiceImplTest {
    private final AiService aiService = mock(AiService.class);
    private final VocabularyArticleMapper mapper = mock(VocabularyArticleMapper.class);
    private final VocabularyArticleServiceImpl service =
            new VocabularyArticleServiceImpl(aiService, mapper, new ObjectMapper());

    @Test
    void doesNotCountSubstringAsTargetWord() {
        when(aiService.generateAnswer(eq(7L), eq("vision"), any()))
                .thenReturn("{\"article\":\"The cart rolled away.\",\"meanings\":{\"art\":\"艺术\"}}");

        VocabularyArticleDtos.ArticleView result = service.generate(
                new VocabularyArticleDtos.GenerateRequest(List.of("art", "cart"), "学习", "中级"), 7L);

        assertThat(result.missingWords()).containsExactly("art");
        assertThat(result.words()).containsExactly("art", "cart");
        verify(aiService, org.mockito.Mockito.times(3)).generateAnswer(eq(7L), eq("vision"), any());
    }

    @Test
    void rejectsInvalidWordBeforeCallingModel() {
        assertThatThrownBy(() -> service.generate(
                new VocabularyArticleDtos.GenerateRequest(List.of("cart", "hello world"), "学习", "中级"), 7L))
                .isInstanceOf(BusinessException.class);

        verify(aiService, never()).generateAnswer(any(), any(), any());
    }

    @Test
    void deduplicatesWordsAcrossScreenshotsAndRetainsUncertainty() throws Exception {
        when(aiService.analyzeImages(eq(7L), eq("vision"), any(), any(), any()))
                .thenReturn("{\"words\":[{\"word\":\"Apple\",\"sourceImageIndex\":0,\"uncertain\":true},"
                        + "{\"word\":\"apple\",\"sourceImageIndex\":1,\"uncertain\":false},"
                        + "{\"word\":\"orbit\",\"sourceImageIndex\":1,\"uncertain\":false},"
                        + "{\"word\":\"button123\",\"sourceImageIndex\":0,\"uncertain\":false}]}");
        byte[] image = samplePng();
        var first = new MockMultipartFile("files", "one.png", "image/png", image);
        var second = new MockMultipartFile("files", "two.png", "image/png", image);

        var extraction = service.extract(List.of(first, second), 7L);

        assertThat(extraction.words()).extracting(VocabularyArticleDtos.CandidateWord::word)
                .containsExactly("Apple", "orbit");
        assertThat(extraction.words().get(0).uncertain()).isTrue();
    }

    private byte[] samplePng() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    @Test
    void reportsVisionUnavailableWhenPersonalConfigurationIsMissing() {
        when(aiService.resolveVisionConfig(7L, "vision"))
                .thenThrow(new BusinessException(400, "请先配置个人 API Key"));

        assertThat(service.visionReady(7L).ready()).isFalse();
        assertThat(service.visionReady(7L).message()).contains("API Key");
        verify(aiService, never()).analyzeImages(any(), any(), any(), any(), any());
    }
}
