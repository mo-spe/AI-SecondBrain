package com.secondbrain.service;

import com.secondbrain.dto.CreateCommunityAnswerRequest;
import com.secondbrain.entity.CommunityQuestion;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.CommunityAnswerMapper;
import com.secondbrain.mapper.CommunityQuestionMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.impl.CommunityQuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 社区问答服务业务边界测试。
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class CommunityQuestionServiceTest {

    @Mock
    private CommunityQuestionMapper questionMapper;

    @Mock
    private CommunityAnswerMapper answerMapper;

    @Mock
    private KnowledgeNodeMapper knowledgeNodeMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SquareNotificationService notificationService;

    private CommunityQuestionService service;

    @BeforeEach
    void setUp() {
        service = new CommunityQuestionServiceImpl(questionMapper, answerMapper, knowledgeNodeMapper,
                userMapper, notificationService);
    }

    @Test
    void shouldRejectKnowledgeOwnedByAnotherUser() {
        CommunityQuestion question = new CommunityQuestion();
        question.setId(10L);
        question.setAuthorId(2L);
        question.setStatus("OPEN");
        when(questionMapper.selectById(10L)).thenReturn(question);

        KnowledgeNode foreignNode = new KnowledgeNode();
        foreignNode.setId(99L);
        foreignNode.setUserId(3L);
        when(knowledgeNodeMapper.selectBatchIds(List.of(99L))).thenReturn(List.of(foreignNode));

        CreateCommunityAnswerRequest request = new CreateCommunityAnswerRequest();
        request.setContent("这是一段满足长度要求的回答内容");
        request.setKnowledgeNodeIds(List.of(99L));

        assertThatThrownBy(() -> service.createAnswer(10L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能引用并公开自己的知识点");
        verify(answerMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAllowOnlyQuestionAuthorToAcceptAnswer() {
        CommunityQuestion question = new CommunityQuestion();
        question.setId(10L);
        question.setAuthorId(2L);
        when(questionMapper.selectById(10L)).thenReturn(question);

        assertThatThrownBy(() -> service.acceptAnswer(10L, 20L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有问题发布者可以采纳回答");
        verify(answerMapper, never()).selectById(20L);
    }
}
