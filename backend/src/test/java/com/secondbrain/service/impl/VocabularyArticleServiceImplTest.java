package com.secondbrain.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.VocabularyArticleDtos;
import com.secondbrain.entity.VocabularyArticle;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.VocabularyArticleMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.CacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

/** 验证文章覆盖结论独立于模型自报内容，并避免无效词表产生费用。 */
class VocabularyArticleServiceImplTest {
    private final AiService aiService = mock(AiService.class);
    private final VocabularyArticleMapper mapper = mock(VocabularyArticleMapper.class);
    private final CacheService cache = mock(CacheService.class);
    private final VocabularyArticleServiceImpl service =
            new VocabularyArticleServiceImpl(aiService, mapper, new ObjectMapper(), cache);

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
    void completesMissingWordsInSavedArticleWithoutReplacingOriginalReading() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setId(12L);
        saved.setUserId(7L);
        saved.setWordsJson("[\"art\",\"cart\"]");
        saved.setArticle("The cart rolled away.");
        saved.setMeaningsJson("{\"cart\":\"手推车\"}");
        saved.setMissingWordsJson("[\"art\"]");
        saved.setTopic("学习");
        saved.setDifficulty("中级");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(mapper.updateById(saved)).thenReturn(1);
        when(aiService.generateAnswer(eq(7L), eq("vision"), any()))
                .thenReturn("{\"article\":\"An art studio opened nearby.\",\"meanings\":{\"art\":\"艺术\"}}");

        VocabularyArticleDtos.ArticleView result = service.completeMissing(12L, 7L);

        assertThat(result.article()).startsWith("The cart rolled away.")
                .contains("补充阅读", "An art studio opened nearby.");
        assertThat(result.missingWords()).isEmpty();
        assertThat(result.meanings()).containsEntry("art", "艺术");
        verify(mapper).updateById(saved);
    }

    @Test
    void keepsReadableDraftWhenRepairAndSupplementHaveInvalidFormat() {
        when(aiService.generateAnswer(eq(7L), eq("vision"), any()))
                .thenReturn("{\"article\":\"A quiet room.\",\"meanings\":{}}", "not-json", "not-json");

        VocabularyArticleDtos.ArticleView result = service.generate(
                new VocabularyArticleDtos.GenerateRequest(List.of("art"), "学习", "中级"), 7L);

        assertThat(result.article()).isEqualTo("A quiet room.");
        assertThat(result.missingWords()).containsExactly("art");
        verify(mapper).insert(any(VocabularyArticle.class));
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

    @Test
    void translatesAllParagraphsAndReusesCacheUntilBodyChanges() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("A habit helps.\n\n补充阅读\n\nA quiet room helps too.");
        when(mapper.selectOne(any())).thenReturn(saved);
        Map<String, String> values = new HashMap<>();
        when(cache.get(anyString(), eq(String.class))).thenAnswer(call -> values.get(call.getArgument(0)));
        doAnswer(call -> {
            values.put(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(cache).set(anyString(), any(), eq(7L), eq(TimeUnit.DAYS));
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString()))
                .thenReturn("{\"translations\":[{\"paragraphIndex\":3,\"translation\":\"安静的房间也有所帮助。\"},"
                        + "{\"paragraphIndex\":1,\"translation\":\"习惯有所帮助。\"}]}",
                        "{\"translations\":[{\"paragraphIndex\":1,\"translation\":\"更新后的正文。\"}]}");

        var first = service.translate(12L, 7L);
        var again = service.translate(12L, 7L);
        assertThat(first.translation()).isEqualTo("习惯有所帮助。\n\n补充阅读\n\n安静的房间也有所帮助。");
        assertThat(again).isEqualTo(first);
        verify(aiService).generateAnswer(eq(7L), eq("vision"), org.mockito.ArgumentMatchers.contains("A quiet room helps too."));
        saved.setArticle("Updated body.");
        var updated = service.translate(12L, 7L);
        assertThat(updated.sourceHash()).isNotEqualTo(first.sourceHash());
        assertThat(updated.translation()).isEqualTo("更新后的正文。");
        verify(aiService, times(2)).generateAnswer(any(), any(), any());
        verify(mapper, never()).updateById(any());
    }

    @Test
    void verifiesOwnershipBeforeLookingUpTranslationCache() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.translate(12L, 8L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权访问");
        verifyNoInteractions(cache, aiService);
    }

    @Test
    void doesNotCachePartialOrEmptyTranslations() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("First paragraph.\n\nSecond paragraph.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(aiService.generateAnswer(eq(7L), eq("vision"), any()))
                .thenReturn("{\"translations\":[\"只翻译了一段\"]}", "{\"translations\":[\"第一段\",\"\"]}");
        assertThatThrownBy(() -> service.translate(12L, 7L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.translate(12L, 7L)).isInstanceOf(BusinessException.class);
        verify(cache, never()).set(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void cacheKeysKeepUsersSeparateAndModelFailuresDoNotOverwriteArticles() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("A habit helps.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(cache.get(org.mockito.ArgumentMatchers.startsWith("vocabulary:translation:v2:7:"), eq(String.class)))
                .thenReturn("缓存译文");
        when(aiService.generateAnswer(eq(8L), eq("vision"), any()))
                .thenThrow(new BusinessException(502, "模型暂不可用"));
        assertThat(service.translate(12L, 7L).translation()).isEqualTo("缓存译文");
        assertThatThrownBy(() -> service.translate(12L, 8L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("模型暂不可用");
        verify(aiService, never()).generateAnswer(eq(7L), any(), any());
        verify(mapper, never()).updateById(any());
    }

    @Test
    void preservesChineseHeadingWhenModelReturnsOnlyEnglishParagraphTranslations() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("A habit helps.\n\n补充阅读\n\nA quiet room helps too.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString()))
                .thenReturn("{\"translations\":[\"习惯有所帮助。\",\"安静的房间也有所帮助。\"]}");

        assertThat(service.translate(12L, 7L).translation())
                .isEqualTo("习惯有所帮助。\n\n补充阅读\n\n安静的房间也有所帮助。");
        verify(aiService).generateAnswer(eq(7L), eq("vision"), anyString());
    }

    @Test
    void repairsOnlyMissingNumberedParagraphAndRestoresOriginalOrder() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("First source.\n\nSecond source.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString()))
                .thenReturn("{\"translations\":[{\"paragraphIndex\":2,\"translation\":\"第二段译文\"}]}",
                        "{\"translations\":[{\"paragraphIndex\":1,\"translation\":\"第一段译文\"}]}");

        assertThat(service.translate(12L, 7L).translation()).isEqualTo("第一段译文\n\n第二段译文");
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(aiService, times(2)).generateAnswer(eq(7L), eq("vision"), prompts.capture());
        assertThat(prompts.getAllValues().get(1)).contains("First source.").doesNotContain("Second source.");
    }

    @Test
    void doesNotGuessAlignmentOfMergedParagraphsAndUsesOneBoundedRepair() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("First source.\n\nSecond source.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString()))
                .thenReturn("{\"translations\":[\"合并后的译文\"]}",
                        "{\"translations\":[{\"paragraphIndex\":1,\"translation\":\"第一段\"},"
                                + "{\"paragraphIndex\":2,\"translation\":\"第二段\"}]}");

        assertThat(service.translate(12L, 7L).translation()).isEqualTo("第一段\n\n第二段");
        verify(aiService, times(2)).generateAnswer(any(), any(), any());
    }

    @Test
    void ambiguousDuplicateIndexIsRepairedWithoutDiscardingOtherParagraphs() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("First source.\n\nSecond source.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString()))
                .thenReturn("{\"translations\":[{\"paragraphIndex\":1,\"translation\":\"歧义一\"},"
                                + "{\"paragraphIndex\":1,\"translation\":\"歧义二\"},"
                                + "{\"paragraphIndex\":2,\"translation\":\"第二段\"}]}",
                        "{\"translations\":[{\"paragraphIndex\":1,\"translation\":\"第一段\"}]}");

        assertThat(service.translate(12L, 7L).translation()).isEqualTo("第一段\n\n第二段");
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(aiService, times(2)).generateAnswer(eq(7L), eq("vision"), prompts.capture());
        assertThat(prompts.getAllValues().get(1)).doesNotContain("Second source.");
    }

    @Test
    void malformedTranslationIsRepairedOnceAndRepeatedFailureRemainsUncached() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("A habit helps.");
        when(mapper.selectOne(any())).thenReturn(saved);
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString())).thenReturn("not-json");

        assertThatThrownBy(() -> service.translate(12L, 7L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("尚缺 1 段");
        verify(aiService, times(2)).generateAnswer(any(), any(), any());
        verify(cache, never()).set(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void chineseOnlySectionDoesNotSpendAModelCall() {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle("补充阅读");
        when(mapper.selectOne(any())).thenReturn(saved);

        assertThat(service.translate(12L, 7L).translation()).isEqualTo("补充阅读");
        verifyNoInteractions(aiService);
    }

    @Test
    void repeatedWordUsesClickedSentenceAndReusesOnlyThatOccurrenceCache() {
        VocabularyArticle saved = readingArticle("A bank lends money.\n\nWe sat on the bank.", "{}");
        Map<String, String> entries = new HashMap<>();
        doAnswer(call -> entries.get(call.getArgument(0))).when(cache).get(anyString(), eq(String.class));
        doAnswer(call -> { entries.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(cache).set(anyString(), anyString(), eq(7L), eq(TimeUnit.DAYS));
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString())).thenReturn("河岸", "银行");
        int second = saved.getArticle().lastIndexOf("bank");
        var request = wordRequest(saved, second, second + 4);
        var result = service.wordMeaning(12L, request, 7L);
        assertThat(result.word()).isEqualTo("bank");
        assertThat(result.sentence()).isEqualTo("We sat on the bank.");
        assertThat(service.wordMeaning(12L, request, 7L).meaning()).isEqualTo("河岸");
        assertThat(service.wordMeaning(12L, wordRequest(saved, 2, 6), 7L).meaning()).isEqualTo("银行");
        ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
        verify(aiService, times(2)).generateAnswer(eq(7L), eq("vision"), prompts.capture());
        assertThat(prompts.getAllValues().get(0)).contains("We sat on the bank.").doesNotContain("lends money");
        assertThat(prompts.getAllValues().get(1)).contains("lends money").doesNotContain("We sat");
        verify(mapper, never()).updateById(any());
    }

    @Test
    void knownTargetMeaningIsReturnedWithoutAnotherModelCall() {
        VocabularyArticle saved = readingArticle("A Habit helps.", "{\"habit\":\"习惯\"}");
        assertThat(service.wordMeaning(12L, wordRequest(saved, 2, 7), 7L).meaning()).isEqualTo("习惯");
        verifyNoInteractions(aiService, cache);
    }

    @Test
    void wordMeaningChecksOwnershipBeforeCacheOrModelAccess() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.wordMeaning(12L,
                new VocabularyArticleDtos.WordMeaningRequest("hash", 0, 1), 8L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权访问");
        verifyNoInteractions(aiService, cache);
    }

    @Test
    void wordMeaningRejectsStaleBodyAndPartialWordPositions() {
        VocabularyArticle saved = readingArticle("A cart rolls.", "{}");
        assertThatThrownBy(() -> service.wordMeaning(12L,
                new VocabularyArticleDtos.WordMeaningRequest("old-hash", 2, 6), 7L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已更新");
        for (int[] range : List.of(new int[]{-1, 2}, new int[]{3, 6}, new int[]{2, 5}, new int[]{6, 7},
                new int[]{0, 999})) {
            assertThatThrownBy(() -> service.wordMeaning(12L, wordRequest(saved, range[0], range[1]), 7L))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("完整英文单词");
        }
        verifyNoInteractions(aiService, cache);
    }

    @Test
    void wordCacheIsIsolatedByUserAndBodyVersion() {
        VocabularyArticle saved = readingArticle("The bank opens.", "{}");
        var first = wordRequest(saved, 4, 8);
        when(cache.get(eq("vocabulary:word-meaning:v1:7:12:" + first.sourceHash() + ":4:8"), eq(String.class)))
                .thenReturn("银行");
        when(aiService.generateAnswer(eq(8L), eq("vision"), anyString())).thenReturn("银行机构");
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString())).thenReturn("更新后的释义");
        assertThat(service.wordMeaning(12L, first, 7L).meaning()).isEqualTo("银行");
        assertThat(service.wordMeaning(12L, first, 8L).meaning()).isEqualTo("银行机构");
        saved.setArticle("The bank closes.");
        assertThat(wordRequest(saved, 4, 8).sourceHash()).isNotEqualTo(first.sourceHash());
        assertThat(service.wordMeaning(12L, wordRequest(saved, 4, 8), 7L).meaning()).isEqualTo("更新后的释义");
        verify(aiService).generateAnswer(eq(7L), eq("vision"), anyString());
    }

    @Test
    void invalidWordMeaningIsNotCachedOrAutomaticallyRetried() {
        VocabularyArticle saved = readingArticle("A cart rolls.", "{}");
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString())).thenReturn("", "English only");
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> service.wordMeaning(12L, wordRequest(saved, 2, 6), 7L))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("有效中文释义");
        }
        verify(aiService, times(2)).generateAnswer(any(), any(), any());
        verify(cache, never()).set(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void wordLookupSupportsCurlyApostrophesAndHyphenatedWordsWithUtf16Offsets() {
        VocabularyArticle saved = readingArticle("🌿 Don’t ignore well-being.", "{}");
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString())).thenReturn("不要", "身心健康");
        int contraction = saved.getArticle().indexOf("Don’t");
        assertThat(service.wordMeaning(12L, wordRequest(saved, contraction, contraction + 5), 7L).word())
                .isEqualTo("Don’t");
        int compound = saved.getArticle().indexOf("well-being");
        assertThat(service.wordMeaning(12L, wordRequest(saved, compound, compound + 10), 7L).word())
                .isEqualTo("well-being");
    }

    @Test
    void wordLookupBoundsContextEvenForVeryLongUnpunctuatedParagraph() {
        VocabularyArticle saved = readingArticle("before ".repeat(200) + "bank " + "after ".repeat(200), "{}");
        when(aiService.generateAnswer(eq(7L), eq("vision"), anyString())).thenReturn("河岸");
        int start = saved.getArticle().indexOf("bank");
        var result = service.wordMeaning(12L, wordRequest(saved, start, start + 4), 7L);
        assertThat(result.sentence()).contains("bank").hasSizeLessThanOrEqualTo(804);
    }

    private VocabularyArticle readingArticle(String body, String meanings) {
        VocabularyArticle saved = new VocabularyArticle();
        saved.setArticle(body);
        saved.setWordsJson("[]");
        saved.setMeaningsJson(meanings);
        saved.setMissingWordsJson("[]");
        when(mapper.selectOne(any())).thenReturn(saved);
        return saved;
    }

    private VocabularyArticleDtos.WordMeaningRequest wordRequest(VocabularyArticle saved, int start, int end) {
        try {
            String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(saved.getArticle().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return new VocabularyArticleDtos.WordMeaningRequest(hash, start, end);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
