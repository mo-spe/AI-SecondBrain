package com.secondbrain.research.orchestrator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 研究预算与低收益熔断测试.
 *
 * @author AI
 */
class ResearchBudgetTest {

    @Test
    void shouldStopAfterTwoRoundsWithoutNewSources() {
        SourceGrowthTracker tracker = new SourceGrowthTracker(3);

        assertThat(tracker.recordAndShouldStop(3)).isFalse();
        assertThat(tracker.recordAndShouldStop(3)).isTrue();
        assertThat(tracker.getConsecutiveStagnantRounds()).isEqualTo(2);
    }

    @Test
    void shouldResetStagnantRoundsWhenNewSourceAppears() {
        SourceGrowthTracker tracker = new SourceGrowthTracker(3);

        assertThat(tracker.recordAndShouldStop(3)).isFalse();
        assertThat(tracker.recordAndShouldStop(4)).isFalse();
        assertThat(tracker.recordAndShouldStop(4)).isFalse();
        assertThat(tracker.getConsecutiveStagnantRounds()).isEqualTo(1);
    }

    @Test
    void shouldEnforceToolCallBudgetAndDetectDuplicates() {
        ToolCallBudget budget = new ToolCallBudget(1);

        budget.recordCall("web_search", "query-hash");

        assertThat(budget.canCall()).isFalse();
        assertThat(budget.isDuplicateCall("web_search", "query-hash")).isTrue();
        assertThatThrownBy(() -> budget.recordCall("web_fetch", "url-hash"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldEnforceTokenBudget() {
        TokenBudget budget = new TokenBudget(100);

        budget.consume(80);
        assertThat(budget.isWarning()).isTrue();
        assertThat(budget.isExceeded()).isFalse();

        budget.consume(20);
        assertThat(budget.isExceeded()).isTrue();
        assertThat(budget.getRemaining()).isZero();
    }
}
