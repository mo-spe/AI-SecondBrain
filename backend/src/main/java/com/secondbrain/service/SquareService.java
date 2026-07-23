package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.vo.SquareCommentVO;
import com.secondbrain.vo.SquarePostVO;
import com.secondbrain.vo.SquareReportVO;

import java.util.List;

/**
 * 知识广场服务接口.
 *
 * <p>提供广场帖子发布、浏览、点赞、评论、收藏、举报及审核功能。</p>
 */
public interface SquareService {

    /**
     * 发布知识节点到广场.
     *
     * @param nodeId        知识节点ID
     * @param recommendText 推荐语（可选，≤200字）
     * @param scope         发布范围：global / workspace
     * @param workspaceId   工作区ID（scope=workspace 时必填）
     * @param userId        当前用户ID
     * @return 创建后的帖子VO
     */
    SquarePostVO publish(Long nodeId, String recommendText, String scope, Long workspaceId, Long userId);

    /**
     * 下架自己的帖子.
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     */
    void unpublish(Long postId, Long userId);

    /**
     * 广场帖子列表（分页 + 排序 + 搜索）.
     *
     * @param scope      发布范围：global / workspace
     * @param sort       排序方式：newest（最新）/ hottest（最热）
     * @param keyword    搜索关键词（匹配推荐语）
     * @param current    当前页
     * @param size       每页大小
     * @param userId     当前用户ID（用于标记点赞/收藏状态）
     * @param workspaceId 工作区ID（scope=workspace 时必填）
     * @return 分页帖子列表
     */
    IPage<SquarePostVO> list(String scope, String sort, String keyword, Integer current, Integer size, Long userId, Long workspaceId);

    /**
     * 帖子详情（含评论列表）.
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return 帖子详情VO
     */
    SquarePostVO getDetail(Long postId, Long userId);

    /**
     * 点赞/取消点赞.
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return true=已点赞，false=已取消
     */
    boolean toggleLike(Long postId, Long userId);

    /**
     * 发表评论.
     *
     * @param postId  帖子ID
     * @param content 评论内容
     * @param userId  当前用户ID
     * @return 评论VO
     */
    SquareCommentVO addComment(Long postId, String content, Long userId);

    /**
     * 删除评论（仅评论者本人可删除）.
     *
     * @param commentId 评论ID
     * @param userId    当前用户ID
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 收藏/取消收藏.
     *
     * @param postId 帖子ID
     * @param userId 当前用户ID
     * @return true=已收藏，false=已取消
     */
    boolean toggleBookmark(Long postId, Long userId);

    /**
     * 我的收藏列表.
     *
     * @param current 当前页
     * @param size    每页大小
     * @param userId  当前用户ID
     * @return 分页帖子列表
     */
    IPage<SquarePostVO> listMyBookmarks(Integer current, Integer size, Long userId);

    /**
     * 我的点赞列表.
     *
     * @param current 当前页
     * @param size    每页大小
     * @param userId  当前用户ID
     * @return 分页帖子列表
     */
    IPage<SquarePostVO> listMyLikes(Integer current, Integer size, Long userId);

    /**
     * 举报帖子.
     *
     * @param postId 帖子ID
     * @param reason 举报原因
     * @param userId 举报人ID
     */
    void report(Long postId, String reason, Long userId);

    /**
     * 举报列表（管理员）.
     *
     * @param status  过滤状态：pending / ignored / removed
     * @param current 当前页
     * @param size    每页大小
     * @return 分页举报列表
     */
    IPage<SquareReportVO> listReports(String status, Integer current, Integer size);

    /**
     * 处理举报（管理员）.
     *
     * @param reportId   举报ID
     * @param action     处理方式：ignore / remove
     * @param handleNote 处理备注
     * @param handlerId  处理人ID
     */
    void handleReport(Long reportId, String action, String handleNote, Long handlerId);

    /**
     * 获取全部敏感词列表.
     *
     * @return 敏感词实体列表
     */
    List<com.secondbrain.entity.SensitiveWord> getSensitiveWords();

    /**
     * 添加敏感词.
     *
     * @param word 敏感词
     */
    void addSensitiveWord(String word);

    /**
     * 删除敏感词.
     *
     * @param id 敏感词ID
     */
    void deleteSensitiveWord(Long id);
}
