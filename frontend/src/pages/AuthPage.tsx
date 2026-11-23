import React, { useState } from 'react';
import { GoogleMark } from '../components/GoogleMark';
import { useLoginMutation, useSignupMutation } from '../services/authApi';
import type { ApiErrorResponse } from '../types/api.types';

export function AuthPage() {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [login, { isLoading: isLoginLoading }] = useLoginMutation();
  const [signup, { isLoading: isSignupLoading }] = useSignupMutation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const isLoading = isLoginLoading || isSignupLoading;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    try {
      if (mode === 'login') {
        const res = await login({
          email: email.trim(),
          password,
        }).unwrap();
        if (!res.success) {
          setErrorMsg(res.message || 'Đăng nhập không thành công.');
        }
      } else {
        if (!fullName.trim()) {
          setErrorMsg('Vui lòng nhập họ và tên.');
          return;
        }
        const res = await signup({
          email: email.trim(),
          password,
          fullName: fullName.trim(),
        }).unwrap();
        if (!res.success) {
          setErrorMsg(res.message || 'Đăng ký không thành công.');
        }
      }
    } catch (err: unknown) {
      const apiErr = err as { data?: ApiErrorResponse; message?: string };
      setErrorMsg(
        apiErr.data?.message ||
          (mode === 'login'
            ? 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.'
            : 'Đăng ký thất bại. Email có thể đã được sử dụng.')
      );
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        {/* Header / Brand */}
        <div className="auth-header">
          <div className="brand-badge">LeeTKOB</div>
          <h1 className="auth-title">
            {mode === 'login' ? 'Chào mừng trở lại' : 'Tạo tài khoản mới'}
          </h1>
          <p className="auth-subtitle">
            Hệ thống xác thực LeeTKOB với JWT và Google OAuth2
          </p>
        </div>

        {/* Tab switch */}
        <div className="tab-group">
          <button
            type="button"
            className={`tab-btn ${mode === 'login' ? 'active' : ''}`}
            onClick={() => {
              setMode('login');
              setErrorMsg(null);
            }}
          >
            Đăng nhập
          </button>
          <button
            type="button"
            className={`tab-btn ${mode === 'signup' ? 'active' : ''}`}
            onClick={() => {
              setMode('signup');
              setErrorMsg(null);
            }}
          >
            Đăng ký
          </button>
        </div>

        {/* Google OAuth Button */}
        <button
          type="button"
          onClick={handleGoogleLogin}
          className="google-btn"
        >
          <GoogleMark />
          <span>Tiếp tục với Google</span>
        </button>

        {/* Divider */}
        <div className="divider">
          <span>HOẶC TIẾP TỤC VỚI EMAIL</span>
        </div>

        {/* Error Alert */}
        {errorMsg && (
          <div className="alert alert-error">
            <span>{errorMsg}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="auth-form">
          {mode === 'signup' && (
            <div className="form-group">
              <label htmlFor="fullName">Họ và tên</label>
              <input
                id="fullName"
                type="text"
                placeholder="Nguyễn Văn A"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </div>
          )}

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Mật khẩu</label>
            <input
              id="password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={6}
            />
            {mode === 'signup' && (
              <span className="field-hint">Mật khẩu tối thiểu 6 ký tự</span>
            )}
          </div>

          <button
            type="submit"
            className="submit-btn"
            disabled={isLoading}
          >
            {isLoading ? (
              <span>Đang xử lý...</span>
            ) : mode === 'login' ? (
              <span>Đăng nhập</span>
            ) : (
              <span>Đăng ký tài khoản</span>
            )}
          </button>
        </form>

        {/* Footer info */}
        <div className="auth-footer">
          {mode === 'login' ? (
            <p>
              Chưa có tài khoản?{' '}
              <button
                type="button"
                className="link-btn"
                onClick={() => {
                  setMode('signup');
                  setErrorMsg(null);
                }}
              >
                Đăng ký ngay
              </button>
            </p>
          ) : (
            <p>
              Đã có tài khoản?{' '}
              <button
                type="button"
                className="link-btn"
                onClick={() => {
                  setMode('login');
                  setErrorMsg(null);
                }}
              >
                Đăng nhập
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
