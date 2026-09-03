package com.secondbrain.service;

import com.secondbrain.entity.CommunityAnswer;
import com.secondbrain.entity.CommunityQuestion;
import com.secondbrain.entity.SquarePost;
import com.secondbrain.entity.User;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.CommunityAnswerMapper;
import com.secondbrain.mapper.CommunityQuestionMapper;
import com.secondbrain.mapper.CommunityUserProfileMapper;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.SquarePostMapper;
import com.secondbrain.mapper.UserBlockMapper;
import com.secondbrain.mapper.UserFollowMapper;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.service.impl.CommunityUserServiceImpl;
import com.secondbrain.vo.CommunityUserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 社区用户资料、关注和拉黑规则测试。
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class CommunityUserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private CommunityUserProfileMapper profileMapper;
    @Mock private UserFollowMapper followMapper;
    @Mock private UserBlockMapper blockMapper;
    @Mock private CommunityQuestionMapper questionMapper;
    @Mock private CommunityAnswerMapper answerMapper;
    @Mock private SquarePostMapper squarePostMapper;
    @Mock private KnowledgeNodeMapper knowledgeNodeMapper;
    @Mock private SquareNotificationService notificationService;

    private CommunityUserService service;

    @BeforeEach
    void setUp() {
        service = new CommunityUserServiceImpl(userMapper, profileMapper, followMapper, blockMapper,
                questionMapper, answerMapper, squarePostMapper, knowledgeNodeMapper, notificationService);
    }

    @Test
    void shouldRejectFollowingSelf() {
        assertThatThrownBy(() -> service.follow(7L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能关注自己");
        verify(followMapper, never()).insert(ArgumentMatchers.any());
    }

    @Test
    void shouldTreatRepeatedFollowAsSuccess() {
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L, "关注者"));
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L, "作者"));
        when(blockMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        when(followMapper.selectCount(ArgumentMatchers.any())).thenReturn(1L);

        service.follow(1L, 2L);

        verify(followMapper, never()).insert(ArgumentMatchers.any());
        verify(notificationService, never()).create(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void shouldRejectFollowWhenEitherUserHasBlockedTheOther() {
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L, "关注者"));
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L, "作者"));
        when(blockMapper.selectCount(ArgumentMatchers.any())).thenReturn(1L);

        assertThatThrownBy(() -> service.follow(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在拉黑关系");
        verify(followMapper, never()).insert(ArgumentMatchers.any());
    }

    @Test
    void shouldRemoveBothFollowDirectionsWhenBlocking() {
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L, "用户一"));
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L, "用户二"));
        when(blockMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);

        service.block(1L, 2L);

        verify(blockMapper).insert(ArgumentMatchers.any());
        verify(followMapper, atLeastOnce()).delete(ArgumentMatchers.any());
    }

    @Test
    void shouldAggregateOnlyPublicCommunityFactsWithoutSensitiveFields() {
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L, "公开昵称"));
        when(profileMapper.selectById(2L)).thenReturn(null);
        when(followMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        when(blockMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);

        CommunityQuestion question = new CommunityQuestion();
        question.setId(10L);
        question.setTitle("公开问题");
        question.setContent("公开问题内容");
        when(questionMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(question));

        CommunityAnswer accepted = new CommunityAnswer();
        accepted.setId(20L);
        accepted.setQuestionId(10L);
        accepted.setAccepted(1);
        accepted.setContent("已采纳回答");
        when(answerMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(accepted));
        when(questionMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(question));

        SquarePost post = new SquarePost();
        post.setId(30L);
        post.setNodeId(40L);
        post.setLikeCount(6);
        post.setBookmarkCount(4);
        when(squarePostMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(post));
        when(knowledgeNodeMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of());

        CommunityUserProfileVO result = service.getProfile(2L, 1L);

        assertThat(result.getQuestionCount()).isEqualTo(1L);
        assertThat(result.getAnswerCount()).isEqualTo(1L);
        assertThat(result.getAcceptedAnswerCount()).isEqualTo(1L);
        assertThat(result.getKnowledgePostCount()).isEqualTo(1L);
        assertThat(result.getReceivedLikeCount()).isEqualTo(6L);
        assertThat(result.getReceivedBookmarkCount()).isEqualTo(4L);
        Set<String> publicFields = Arrays.stream(CommunityUserProfileVO.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
        assertThat(publicFields).doesNotContain("password", "email", "phone", "apiKey");
    }

    private User activeUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(1);
        return user;
    }
}
