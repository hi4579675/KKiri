package com.kkiri.backend.auth.application;

import com.kkiri.backend.auth.application.dto.CompleteProfileRequest;
import com.kkiri.backend.auth.application.dto.UpdateProfileRequest;
import com.kkiri.backend.auth.domain.User;
import com.kkiri.backend.auth.infrastructure.UserRepository;
import com.kkiri.backend.global.exception.CustomException;
import com.kkiri.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ── 닉네임 중복 확인 ──────────────────────────────────────────

    @Test
    @DisplayName("사용 가능한 닉네임이면 true 반환")
    void isNicknameAvailable_available() {
        given(userRepository.existsByNickname("끼리")).willReturn(false);

        boolean result = userService.isNicknameAvailable("끼리");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 false 반환")
    void isNicknameAvailable_duplicate() {
        given(userRepository.existsByNickname("끼리")).willReturn(true);

        boolean result = userService.isNicknameAvailable("끼리");

        assertThat(result).isFalse();
    }

    // ── 프로필 최초 설정 ──────────────────────────────────────────

    @Test
    @DisplayName("프로필 설정 성공")
    void completeProfile_success() {
        User user = User.createPending("kakao123");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("끼리")).willReturn(false);

        CompleteProfileRequest request = new CompleteProfileRequest("끼리", "🐶", "#FF5733");
        userService.completeProfile(1L, request);

        assertThat(user.isProfileCompleted()).isTrue();
        assertThat(user.getNickname()).isEqualTo("끼리");
    }

    @Test
    @DisplayName("중복 닉네임으로 프로필 설정 시 예외 발생")
    void completeProfile_duplicateNickname() {
        User user = User.createPending("kakao123");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("끼리")).willReturn(true);

        CompleteProfileRequest request = new CompleteProfileRequest("끼리", "🐶", "#FF5733");

        assertThatThrownBy(() -> userService.completeProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("존재하지 않는 유저 프로필 설정 시 예외 발생")
    void completeProfile_userNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        CompleteProfileRequest request = new CompleteProfileRequest("끼리", "🐶", "#FF5733");

        assertThatThrownBy(() -> userService.completeProfile(999L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.USER_NOT_FOUND);
    }

    // ── 프로필 수정 ──────────────────────────────────────────────

    @Test
    @DisplayName("닉네임 변경 없이 프로필 수정 성공")
    void updateProfile_sameNickname() {
        User user = User.createPending("kakao123");
        user.completeProfile("끼리", "🐶", "#FF5733");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest("끼리", "🐱", "#000000");
        userService.updateProfile(1L, request);

        assertThat(user.getAvatarEmoji()).isEqualTo("🐱");
        assertThat(user.getAvatarColor()).isEqualTo("#000000");
    }

    @Test
    @DisplayName("닉네임 변경 시 중복이면 예외 발생")
    void updateProfile_duplicateNickname() {
        User user = User.createPending("kakao123");
        user.completeProfile("끼리", "🐶", "#FF5733");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNickname("새닉네임")).willReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", "🐱", "#000000");

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("baseErrorCode", ErrorCode.DUPLICATE_NICKNAME);
    }
}
