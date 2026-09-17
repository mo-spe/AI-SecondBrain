package com.secondbrain.controller;

import com.secondbrain.dto.UpdateCommunityProfileRequest;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.service.CommunityUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 社区用户接口身份边界测试。
 *
 * @author AI
 */
@ExtendWith(MockitoExtension.class)
class CommunityUserControllerTest {

    @Mock
    private CommunityUserService communityUserService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Test
    void shouldRejectUnauthenticatedProfileAccess() {
        CommunityUserController controller = new CommunityUserController(communityUserService);

        assertThatThrownBy(() -> controller.profile(2L, httpServletRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(401);
    }

    @Test
    void shouldAlwaysUpdateProfileForAuthenticatedUser() {
        CommunityUserController controller = new CommunityUserController(communityUserService);
        UpdateCommunityProfileRequest request = new UpdateCommunityProfileRequest();
        when(httpServletRequest.getAttribute("userId")).thenReturn(7L);

        controller.updateMyProfile(request, httpServletRequest);

        verify(communityUserService).updateProfile(7L, request);
    }
}
