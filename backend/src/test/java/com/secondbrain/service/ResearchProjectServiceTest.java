package com.secondbrain.service;

import com.secondbrain.dto.CreateResearchProjectRequest;
import com.secondbrain.dto.UpdateResearchProjectRequest;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.vo.ResearchProjectVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 研究项目服务层集成测试.
 *
 * <p>测试 CRUD、状态机验证、用户隔离和分页.</p>
 *
 * @author AI
 */
@SpringBootTest
@DisplayName("研究项目服务层测试")
class ResearchProjectServiceTest {

    @Autowired
    private ResearchProjectService researchProjectService;

    private static final Long TEST_USER_ID = 5L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long TEST_WORKSPACE_ID = null; // 个人空间

    // ======================== CRUD ========================

    @Nested
    @DisplayName("CRUD 操作")
    class Crud {

        @Test
        @DisplayName("创建 → 查询 → 更新 → 删除 完整流程")
        void fullCrudLifecycle() {
            // 创建
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("Service 层测试项目");
            req.setGoal("验证完整的 CRUD 流程");

            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);
            assertNotNull(created, "创建结果不应为 null");
            assertNotNull(created.getId(), "ID 不应为 null");
            assertEquals("Service 层测试项目", created.getTitle(), "标题应匹配");
            assertEquals("草稿", created.getStatusLabel(), "新项目应为草稿状态");
            assertEquals("DRAFT", created.getStatus());

            Long id = created.getId();

            // 查询详情
            ResearchProjectVO fetched = researchProjectService.getById(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertNotNull(fetched, "查询结果不应为 null");
            assertEquals(id, fetched.getId());
            assertEquals("验证完整的 CRUD 流程", fetched.getGoal());

            // 更新
            UpdateResearchProjectRequest update = new UpdateResearchProjectRequest();
            update.setTitle("Service 层测试项目 - 已更新");
            ResearchProjectVO updated = researchProjectService.update(id, update, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertEquals("Service 层测试项目 - 已更新", updated.getTitle(), "标题应已更新");

            // 删除
            researchProjectService.delete(id, TEST_USER_ID, TEST_WORKSPACE_ID);

            // 验证删除后不可查
            assertThrows(BusinessException.class, () -> {
                researchProjectService.getById(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            }, "删除后查询应抛出 BusinessException");
        }
    }

    // ======================== 分页 ========================

    @Nested
    @DisplayName("分页查询")
    class Pagination {

        @Test
        @DisplayName("首页查询返回非空结果")
        void shouldReturnFirstPage() {
            Page<ResearchProjectVO> page = researchProjectService.list(
                    1, 10, null, null, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertNotNull(page, "分页结果不应为 null");
            assertTrue(page.getTotal() >= 0, "总数应 >= 0");
            assertEquals(1, page.getCurrent());
        }

        @Test
        @DisplayName("关键词搜索")
        void shouldFilterByKeyword() {
            // 先创建一个有独特关键词的项目
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("分页搜索测试_唯一关键词");
            req.setGoal("分页搜索目标");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);

            Page<ResearchProjectVO> page = researchProjectService.list(
                    1, 10, null, "唯一关键词", TEST_USER_ID, TEST_WORKSPACE_ID);
            assertTrue(page.getTotal() >= 1, "应找到至少 1 个匹配项目");

            researchProjectService.delete(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("状态筛选")
        void shouldFilterByStatus() {
            Page<ResearchProjectVO> page = researchProjectService.list(
                    1, 10, "COMPLETED", null, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertNotNull(page);
            page.getRecords().forEach(project ->
                    assertEquals("COMPLETED", project.getStatus(), "筛选结果状态应匹配"));
        }
    }

    // ======================== 状态机 ========================

    @Nested
    @DisplayName("状态流转")
    class StateTransitions {

        @Test
        @DisplayName("DRAFT → execute → RESEARCHING")
        void shouldTransitionFromDraftToResearching() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("状态机测试 - DRAFT");
            req.setGoal("测试状态流转");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);

            ResearchProjectVO executed = researchProjectService.execute(
                    created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
            assertEquals("RESEARCHING", executed.getStatus(), "execute 后应为 RESEARCHING");
            assertEquals("研究中", executed.getStatusLabel());

            researchProjectService.delete(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("RESEARCHING → pause → PAUSED")
        void shouldTransitionFromResearchingToPaused() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("状态机测试 - pause");
            req.setGoal("测试暂停");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);
            Long id = created.getId();

            researchProjectService.execute(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            researchProjectService.pause(id, TEST_USER_ID, TEST_WORKSPACE_ID);

            ResearchProjectVO paused = researchProjectService.getById(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertEquals("PAUSED", paused.getStatus(), "pause 后应为 PAUSED");

            researchProjectService.delete(id, TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("PAUSED → resume → RESEARCHING")
        void shouldTransitionFromPausedToResearching() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("状态机测试 - resume");
            req.setGoal("测试恢复");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);
            Long id = created.getId();

            researchProjectService.execute(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            researchProjectService.pause(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            ResearchProjectVO resumed = researchProjectService.resume(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertEquals("RESEARCHING", resumed.getStatus(), "resume 后应为 RESEARCHING");

            researchProjectService.delete(id, TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("DRAFT 状态下 pause 被拒绝")
        void shouldRejectPauseOnDraft() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("状态机测试 - 非法 pause");
            req.setGoal("测试");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);

            assertThrows(BusinessException.class, () -> {
                researchProjectService.pause(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
            }, "DRAFT 状态不能暂停");

            researchProjectService.delete(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("DRAFT 状态下 resume 被拒绝")
        void shouldRejectResumeOnDraft() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("状态机测试 - 非法 resume");
            req.setGoal("测试");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);

            assertThrows(BusinessException.class, () -> {
                researchProjectService.resume(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
            }, "DRAFT 状态不能恢复");

            researchProjectService.delete(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("COMPLETED 状态下 execute 被拒绝")
        void shouldRejectExecuteOnCompleted() throws InterruptedException {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("状态机测试 - COMPLETED execute");
            req.setGoal("测试");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);
            Long id = created.getId();

            // 启动研究（异步编排器会自动完成，因为无 plan）
            researchProjectService.execute(id, TEST_USER_ID, TEST_WORKSPACE_ID);

            // 等待异步编排器执行完成
            Thread.sleep(3000);

            assertThrows(BusinessException.class, () -> {
                researchProjectService.execute(id, TEST_USER_ID, TEST_WORKSPACE_ID);
            }, "COMPLETED 状态不能再 execute");

            researchProjectService.delete(id, TEST_USER_ID, TEST_WORKSPACE_ID);
        }
    }

    // ======================== 用户隔离 ========================

    @Nested
    @DisplayName("用户隔离")
    class UserIsolation {

        @Test
        @DisplayName("其他用户不能访问我的项目")
        void shouldRejectOtherUserAccess() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("用户隔离测试");
            req.setGoal("隔离测试");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);

            // 其他用户尝试读取
            assertThrows(BusinessException.class, () -> {
                researchProjectService.getById(created.getId(), OTHER_USER_ID, TEST_WORKSPACE_ID);
            }, "其他用户不应能访问我的项目");

            // 其他用户尝试更新
            UpdateResearchProjectRequest update = new UpdateResearchProjectRequest();
            update.setTitle("恶意修改");
            assertThrows(BusinessException.class, () -> {
                researchProjectService.update(created.getId(), update, OTHER_USER_ID, TEST_WORKSPACE_ID);
            }, "其他用户不应能更新我的项目");

            // 其他用户尝试删除
            assertThrows(BusinessException.class, () -> {
                researchProjectService.delete(created.getId(), OTHER_USER_ID, TEST_WORKSPACE_ID);
            }, "其他用户不应能删除我的项目");

            // 清理
            researchProjectService.delete(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("404 和 403 返回正确的错误码")
        void shouldReturnCorrectErrorCodes() {
            // 404
            BusinessException notFound = assertThrows(BusinessException.class, () -> {
                researchProjectService.getById(99999L, TEST_USER_ID, TEST_WORKSPACE_ID);
            });
            assertEquals(404, notFound.getCode(), "不存在的项目应返回 404");

            // 403
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("错误码测试");
            req.setGoal("测试");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);

            BusinessException forbidden = assertThrows(BusinessException.class, () -> {
                researchProjectService.getById(created.getId(), OTHER_USER_ID, TEST_WORKSPACE_ID);
            });
            assertEquals(403, forbidden.getCode(), "无权限应返回 403");

            researchProjectService.delete(created.getId(), TEST_USER_ID, TEST_WORKSPACE_ID);
        }
    }

    // ======================== 更新语义 ========================

    @Nested
    @DisplayName("更新操作")
    class Updates {

        @Test
        @DisplayName("部分更新 — 仅更新 title，goal 不变")
        void shouldPartialUpdate() {
            CreateResearchProjectRequest req = new CreateResearchProjectRequest();
            req.setTitle("原始标题");
            req.setGoal("原始目标");
            ResearchProjectVO created = researchProjectService.create(req, TEST_USER_ID);
            Long id = created.getId();

            UpdateResearchProjectRequest update = new UpdateResearchProjectRequest();
            update.setTitle("新标题");

            ResearchProjectVO updated = researchProjectService.update(id, update, TEST_USER_ID, TEST_WORKSPACE_ID);
            assertEquals("新标题", updated.getTitle(), "标题应更新");
            assertEquals("原始目标", updated.getGoal(), "goal 不应被覆盖");

            researchProjectService.delete(id, TEST_USER_ID, TEST_WORKSPACE_ID);
        }

        @Test
        @DisplayName("更新不存在的项目返回 404")
        void shouldReturn404ForUpdate() {
            UpdateResearchProjectRequest update = new UpdateResearchProjectRequest();
            update.setTitle("测试");

            assertThrows(BusinessException.class, () -> {
                researchProjectService.update(99999L, update, TEST_USER_ID, TEST_WORKSPACE_ID);
            }, "更新不存在的项目应抛异常");
        }
    }
}
