package UI;

import com.bupt.tarecruitment.auth.AccountStatus;
import com.bupt.tarecruitment.auth.UserAccount;
import com.bupt.tarecruitment.auth.UserRole;

public final class SessionState {
    private String userId;
    private String displayName;
    private UserRole role;
    private AccountStatus status;

    public void setCurrentUser(UserAccount account) {
        userId = account.userId();
        displayName = account.displayName();
        role = account.role();
        status = account.status();
    }

    public void clear() {
        userId = null;
        displayName = null;
        role = null;
        status = null;
    }

    public boolean isAuthenticated() {
        return userId != null && role != null && status == AccountStatus.ACTIVE;
    }

    public String userId() {
        return userId;
    }

    public String displayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return userId == null ? "Guest" : userId;
    }

    public UserRole role() {
        return role;
    }

    public AccountStatus status() {
        return status;
    }
}
