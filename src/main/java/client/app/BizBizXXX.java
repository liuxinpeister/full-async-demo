package client.app;

import server.contract.UserInfo;

// 实在不知道起个啥名
public class BizBizXXX {

    private UserInfo userInfo;

    private long time;

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "BizBizXXX{" +
                "userInfo=" + userInfo +
                ", time=" + time +
                '}';
    }
}
