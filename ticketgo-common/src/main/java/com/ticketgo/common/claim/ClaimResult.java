package com.ticketgo.common.claim;

import lombok.Getter;

/**
 * 通用领取结果码
 */
@Getter
public enum ClaimResult {

    SUCCESS(1, "领取成功"),
    STOCK_EMPTY(0, "已售罄"),
    NOT_WARMED(-1, "库存未预热或活动未开始"),
    LIMIT_REACHED(-2, "超出限购数量");

    private final int code;
    private final String message;

    ClaimResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ClaimResult of(Long code) {
        if (code == null) {
            return NOT_WARMED;
        }
        for (ClaimResult r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        return NOT_WARMED;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
