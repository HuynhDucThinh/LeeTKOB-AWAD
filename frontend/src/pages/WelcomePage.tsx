import { useAppSelector } from '../store/hooks';
import { useLogoutMutation } from '../services/authApi';

export function WelcomePage() {
  const { user } = useAppSelector((state) => state.auth);
  const [logout, { isLoading: isLoggingOut }] = useLogoutMutation();

  const handleLogout = async () => {
    try {
      await logout().unwrap();
    } catch {
      // Handled by authSlice logout matcher
    }
  };

  const displayName = user?.fullName || user?.email || 'Người dùng LeeTKOB';
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <div className="welcome-wrapper">
      <div className="welcome-card">
        {/* Top bar */}
        <div className="welcome-top">
          <span className="welcome-badge">LeeTKOB Workspace</span>
          <button
            type="button"
            onClick={handleLogout}
            disabled={isLoggingOut}
            className="logout-btn"
          >
            {isLoggingOut ? 'Đang thoát...' : 'Đăng xuất'}
          </button>
        </div>

        {/* Hero greeting */}
        <div className="welcome-hero">
          {user?.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt={displayName}
              className="welcome-avatar"
              referrerPolicy="no-referrer"
            />
          ) : (
            <div className="welcome-avatar-fallback">{initial}</div>
          )}

          <h1 className="welcome-title">Chào mừng, {displayName}!</h1>
          <p className="welcome-desc">
            Bạn đã đăng nhập thành công vào nền tảng <strong>LeeTKOB</strong>.
          </p>
        </div>

        {/* User profile details */}
        <div className="profile-details">
          <h3>Thông tin tài khoản</h3>

          <div className="detail-row">
            <span className="detail-label">Email:</span>
            <span className="detail-value">{user?.email}</span>
          </div>

          <div className="detail-row">
            <span className="detail-label">Họ và tên:</span>
            <span className="detail-value">{user?.fullName || 'Chưa thiết lập'}</span>
          </div>

          <div className="detail-row">
            <span className="detail-label">Mã tài khoản (User ID):</span>
            <span className="detail-value mono">{user?.id}</span>
          </div>

          {user?.createdAt && (
            <div className="detail-row">
              <span className="detail-label">Ngày tham gia:</span>
              <span className="detail-value">
                {new Date(user.createdAt).toLocaleDateString('vi-VN', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </span>
            </div>
          )}
        </div>

        <div className="welcome-footer-box">
          <p>Trạng thái: 🟢 Đã xác thực qua Backend Spring Boot & Cookie JWT</p>
        </div>
      </div>
    </div>
  );
}
