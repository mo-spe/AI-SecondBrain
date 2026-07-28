package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.BatchConfirmRequest;
import com.secondbrain.dto.UpdateKnowledgeRequest;
import com.secondbrain.entity.EditingLock;
import com.secondbrain.entity.KnowledgeRevision;
import com.secondbrain.entity.PendingKnowledge;
import com.secondbrain.entity.User;
import com.secondbrain.service.EditingLockService;
import com.secondbrain.service.KnowledgeRevisionService;
import com.secondbrain.service.KnowledgeService;
import com.secondbrain.service.PendingKnowledgeService;
import com.secondbrain.service.UserService;
import com.secondbrain.vo.KnowledgeNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 知识管理控制器. <p>知识节点管理相关接口</p> */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识管理", description = "知识节点管理相关接口")
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeService knowledgeService;
    private final UserService userService;
    private final KnowledgeRevisionService knowledgeRevisionService;
    private final EditingLockService editingLockService;
    private final PendingKnowledgeService pendingKnowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService, UserService userService,
                               KnowledgeRevisionService knowledgeRevisionService,
                               EditingLockService editingLockService,
                               PendingKnowledgeService pendingKnowledgeService) {
        this.knowledgeService = knowledgeService;
        this.userService = userService;
        this.knowledgeRevisionService = knowledgeRevisionService;
        this.editingLockService = editingLockService;
        this.pendingKnowledgeService = pendingKnowledgeService;
    }

    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    /**
     * 知识列表.
     *
     * @param current 当前页
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @param importance 重要程度
     * @param masteryLevel 掌握程度
     * @param httpRequest HTTP请求对象
     * @return 知识分页数据
     */
    @GetMapping("/list")
    @Operation(summary = "知识列表", description = "分页查询知识列表")
    public Result<Page<KnowledgeNodeVO>> list(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "重要程度") @RequestParam(required = false) Integer importance,
            @Parameter(description = "掌握程度") @RequestParam(required = false) Integer masteryLevel,
            @Parameter(description = "标签ID") @RequestParam(required = false) Long tagId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        Page<KnowledgeNodeVO> page = knowledgeService.list(current, size, keyword, userId, importance, masteryLevel, workspaceId, tagId);
        return Result.success(page);
    }

    /**
     * 知识详情.
     *
     * @param id 知识ID
     * @param httpRequest HTTP请求对象
     * @return 知识详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "知识详情", description = "根据ID查询知识详情")
    public Result<KnowledgeNodeVO> getById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        KnowledgeNodeVO vo = knowledgeService.getById(id, userId, workspaceId);
        return Result.success(vo);
    }

    /**
     * 创建知识.
     *
     * @param request 创建知识请求
     * @param httpRequest HTTP请求对象
     * @return 创建的知识详情
     */
    @PostMapping
    @Operation(summary = "创建知识", description = "创建新的知识点")
    public Result<KnowledgeNodeVO> create(@RequestBody UpdateKnowledgeRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        KnowledgeNodeVO vo = knowledgeService.create(request.getTitle(), request.getSummary(), request.getContentMd(), request.getImportance(), userId, workspaceId);
        return Result.success(vo);
    }

    /**
     * 删除知识.
     *
     * @param id 知识ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识", description = "根据ID删除知识")
    public Result<Void> deleteById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        knowledgeService.deleteById(id, userId, workspaceId);
        return Result.success("删除成功", null);
    }

    /**
     * 更新重要程度.
     *
     * @param id 知识ID
     * @param importance 重要程度
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/{id}/importance")
    @Operation(summary = "更新重要程度", description = "更新知识点的重要程度")
    public Result<Void> updateImportance(
            @PathVariable Long id,
            @RequestParam Integer importance,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        knowledgeService.updateImportance(id, importance, userId, workspaceId);
        return Result.success("更新成功", null);
    }

    /**
     * 更新知识点.
     *
     * @param id 知识ID
     * @param request 更新知识请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识点", description = "更新知识点的内容")
    public Result<Void> updateKnowledge(
            @PathVariable Long id,
            @RequestBody UpdateKnowledgeRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);

        // 检查编辑锁：如果存在他人持有的锁则拒绝更新
        EditingLock lock = editingLockService.getLockStatus(id);
        if (lock != null && !lock.getUserId().equals(userId)) {
            User holder = userService.getUserById(lock.getUserId());
            String holderName = holder != null ? holder.getUsername() : "其他用户";
            return Result.error(409, holderName + " 正在编辑此知识点，请稍后再试");
        }

        knowledgeService.updateKnowledge(id, request.getTitle(), request.getSummary(), request.getContentMd(), userId, workspaceId);

        // 更新成功后自动保存版本快照
        knowledgeRevisionService.saveRevision(id, userId, request.getTitle(), request.getContentMd(), request.getSummary());

        if (request.getImportance() != null) {
            knowledgeService.updateImportance(id, request.getImportance(), userId, workspaceId);
        }

        return Result.success("更新成功", null);
    }

    /**
     * 搜索知识点.
     *
     * @param keyword 搜索关键词
     * @param httpRequest HTTP请求对象
     * @return 知识列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索知识点", description = "关键词搜索知识点")
    public Result<List<KnowledgeNodeVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        List<KnowledgeNodeVO> results = knowledgeService.search(keyword, userId, workspaceId);
        return Result.success(results);
    }

    /**
     * 多字段搜索知识点.
     *
     * @param keyword 搜索关键词
     * @param httpRequest HTTP请求对象
     * @return 知识列表
     */
    @GetMapping("/search/multi")
    @Operation(summary = "多字段搜索知识点", description = "在标题、摘要、内容中搜索")
    public Result<List<KnowledgeNodeVO>> multiFieldSearch(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Long workspaceId = getWorkspaceId(httpRequest);
        List<KnowledgeNodeVO> results = knowledgeService.multiFieldSearch(keyword, userId, workspaceId);
        return Result.success(results);
    }

    /**
     * 语义搜索知识点.
     *
     * @param queryText 搜索文本
     * @param topK 返回结果数量
     * @param httpRequest HTTP请求对象
     * @return 知识列表
     */
    @GetMapping("/search/semantic")
    @Operation(summary = "语义搜索知识点", description = "使用向量相似度进行语义搜索")
    public Result<List<KnowledgeNodeVO>> semanticSearch(
            @Parameter(description = "搜索文本") @RequestParam String queryText,
            @Parameter(description = "返回结果数量") @RequestParam(defaultValue = "10") Integer topK,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        Long workspaceId = getWorkspaceId(httpRequest);

        String userApiKey = null;
        try {
            User user = userService.getUserById(userId);
            if (user != null) {
                userApiKey = user.getApiKey();
            }
        } catch (RuntimeException e) {
            log.warn("获取用户 API Key 失败：userId={}, error={}", userId, e.getMessage());
        }

        List<KnowledgeNodeVO> results = knowledgeService.semanticSearch(queryText, userId, topK, userApiKey, workspaceId);
        return Result.success(results);
    }

    /**
     * 同步到Elasticsearch.
     *
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping("/sync-to-elasticsearch")
    @Operation(summary = "同步到Elasticsearch", description = "将所有知识点同步到Elasticsearch")
    public Result<Void> syncToElasticsearch(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        knowledgeService.syncToElasticsearch(userId, workspaceId);
        return Result.success("同步成功", null);
    }

    /**
     * 版本历史列表.
     *
     * @param nodeId      知识节点ID
     * @param httpRequest HTTP请求对象
     * @return 版本历史列表
     */
    @GetMapping("/{nodeId}/revisions")
    @Operation(summary = "版本历史列表", description = "获取知识节点的版本历史记录")
    public Result<List<KnowledgeRevision>> getRevisionList(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        KnowledgeNodeVO vo = knowledgeService.getById(nodeId, userId, workspaceId);
        if (vo == null) {
            return Result.error(404, "知识点不存在");
        }
        List<KnowledgeRevision> revisions = knowledgeRevisionService.getRevisionList(nodeId);
        return Result.success(revisions);
    }

    /**
     * 版本详情.
     *
     * @param nodeId      知识节点ID
     * @param revId       版本ID
     * @param httpRequest HTTP请求对象
     * @return 版本详情
     */
    @GetMapping("/{nodeId}/revisions/{revId}")
    @Operation(summary = "版本详情", description = "获取指定版本的详细内容")
    public Result<KnowledgeRevision> getRevisionDetail(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            @Parameter(description = "版本ID") @PathVariable Long revId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        KnowledgeNodeVO vo = knowledgeService.getById(nodeId, userId, workspaceId);
        if (vo == null) {
            return Result.error(404, "知识点不存在");
        }
        KnowledgeRevision revision = knowledgeRevisionService.getRevisionDetail(revId);
        if (revision == null || !revision.getNodeId().equals(nodeId)) {
            return Result.error(404, "版本不存在");
        }
        return Result.success(revision);
    }

    /**
     * 回滚到指定版本.
     * 仅工作区owner/admin可执行回滚操作。
     *
     * @param nodeId      知识节点ID
     * @param revId       目标版本ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping("/{nodeId}/revisions/{revId}/rollback")
    @Operation(summary = "回滚到指定版本", description = "将知识节点内容恢复到指定版本的状态")
    public Result<Void> rollbackRevision(
            @Parameter(description = "知识节点ID") @PathVariable Long nodeId,
            @Parameter(description = "版本ID") @PathVariable Long revId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        KnowledgeNodeVO vo = knowledgeService.getById(nodeId, userId, workspaceId);
        if (vo == null) {
            return Result.error(404, "知识点不存在");
        }
        knowledgeRevisionService.rollback(nodeId, revId, userId);
        return Result.success("回滚成功", null);
    }

    // ========== 待确认知识点 ==========

    /**
     * 获取待确认知识点列表.
     *
     * @param httpRequest HTTP请求对象
     * @return 待确认知识点列表
     */
    @GetMapping("/pending")
    @Operation(summary = "待确认知识点列表", description = "获取当前工作区下所有待确认的知识点")
    public Result<List<PendingKnowledge>> listPending(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        List<PendingKnowledge> list = pendingKnowledgeService.listPending(userId, workspaceId);
        return Result.success(list);
    }

    /**
     * 批量确认入库.
     *
     * @param request     批量确认请求
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @PostMapping("/pending/confirm")
    @Operation(summary = "批量确认入库", description = "将选中的待确认知识点迁移到知识库，可选是否生成复习卡片")
    public Result<Void> confirmPending(@RequestBody BatchConfirmRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        pendingKnowledgeService.confirmBatch(userId, request);
        return Result.success("确认入库成功", null);
    }

    /**
     * 丢弃单条待确认知识点.
     *
     * @param id          待确认记录ID
     * @param httpRequest HTTP请求对象
     * @return void
     */
    @DeleteMapping("/pending/{id}")
    @Operation(summary = "丢弃待确认知识点", description = "将指定待确认知识点标记为已丢弃")
    public Result<Void> discardPending(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        pendingKnowledgeService.discardPending(userId, id);
        return Result.success("已丢弃", null);
    }

    /**
     * 手动新增待确认知识点.
     *
     * @param item        待确认知识点
     * @param httpRequest HTTP请求对象
     * @return 新增的待确认记录
     */
    @PostMapping("/pending/add")
    @Operation(summary = "手动新增待确认知识点", description = "在待确认列表中手动添加一条知识点")
    public Result<PendingKnowledge> addPending(@RequestBody PendingKnowledge item, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        Long workspaceId = getWorkspaceId(httpRequest);
        item.setWorkspaceId(workspaceId);
        PendingKnowledge result = pendingKnowledgeService.addPending(userId, item);
        return Result.success(result);
    }
}
