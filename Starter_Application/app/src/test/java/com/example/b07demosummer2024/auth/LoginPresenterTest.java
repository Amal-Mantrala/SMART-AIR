package com.example.b07demosummer2024.auth;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class LoginPresenterTest {
    private LoginContract.View mockView;
    private IAuthService mockAuthService;
    private LoginPresenter presenter;

    @Before
    public void setUp() {
        mockView = mock(LoginContract.View.class);
        mockAuthService = mock(IAuthService.class);

        presenter = new LoginPresenter(mockView, mockAuthService);
    }
    @Test
    public void login_invalidEmail_showsEmailError() {
        presenter.onLoginClicked("bademail", "123456");

        verify(mockView).showEmailError(anyString());
        verify(mockAuthService, never()).signIn(anyString(), anyString(), any());
    }

    @Test
    public void login_invalidPassword_showsPasswordError() {
        presenter.onLoginClicked("test@gmail.com", "12");

        verify(mockView).showPasswordError(anyString());
        verify(mockAuthService, never()).signIn(anyString(), anyString(), any());
    }

    @Test
    public void login_success_callsNavigate() {
        String email = "test@gmail.com";
        String password = "123456";

        presenter.onLoginClicked(email, password);

        ArgumentCaptor<AuthService.AuthCallback> callbackCaptor =
                ArgumentCaptor.forClass(AuthService.AuthCallback.class);

        verify(mockAuthService).signIn(eq(email), eq(password), callbackCaptor.capture());

        AuthService.AuthCallback callback = callbackCaptor.getValue();

        callback.onResult(true, "ok");

        verify(mockView).navigateToHome();
        verify(mockView, never()).showLoginError(anyString());
    }

    @Test
    public void login_failure_showsLoginError() {
        String email = "test@gmail.com";
        String password = "123456";

        presenter.onLoginClicked(email, password);

        ArgumentCaptor<AuthService.AuthCallback> callbackCaptor =
                ArgumentCaptor.forClass(AuthService.AuthCallback.class);

        verify(mockAuthService).signIn(eq(email), eq(password), callbackCaptor.capture());

        AuthService.AuthCallback callback = callbackCaptor.getValue();

        callback.onResult(false, "Wrong password");

        verify(mockView).showLoginError("Wrong password");
        verify(mockView, never()).navigateToHome();
    }

    @Test
    public void forgotPassword_invalidEmail_showsError() {
        presenter.onForgotPasswordClicked("bademail");

        verify(mockView).showEmailError(anyString());
        verify(mockAuthService, never()).resetPassword(anyString(), any());
    }

    @Test
    public void forgotPassword_success_showsMessage() {
        String email = "test@gmail.com";

        presenter.onForgotPasswordClicked(email);

        ArgumentCaptor<AuthService.AuthCallback> callbackCaptor =
                ArgumentCaptor.forClass(AuthService.AuthCallback.class);

        verify(mockAuthService).resetPassword(eq(email), callbackCaptor.capture());

        AuthService.AuthCallback callback = callbackCaptor.getValue();

        callback.onResult(true, "Reset sent");

        verify(mockView).showLoginError("Reset sent");
    }
}
