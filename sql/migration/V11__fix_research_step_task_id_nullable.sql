-- ============================================================
-- Fix: research_step.task_id 改为可空
-- 文件：V11__fix_research_step_task_id_nullable.sql
-- 描述：项目级步骤（Planner/Synthesizer）不关联具体 task
-- 日期：2026-07-28
-- ============================================================
USE second_brain;

ALTER TABLE research_step
    MODIFY task_id BIGINT NULL COMMENT '所属任务 ID（项目级步骤可为 NULL）';
