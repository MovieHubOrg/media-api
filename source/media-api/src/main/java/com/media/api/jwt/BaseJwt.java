package com.media.api.jwt;

import com.media.api.utils.ZipUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Data
public class BaseJwt implements Serializable {
    public static final String DELIM = "|";
    public static final String EMPTY_STRING = "<>";
    private Long tokenId;

    private Long accountId = -1L;
    private String kind = EMPTY_STRING; // token kind
    private String permission = EMPTY_STRING;
    private Integer userKind = -1; // loại user là admin hay là gì
    private String username = EMPTY_STRING; // username
    private Boolean isSuperAdmin = false;

    public String toClaim() {
        if (userKind == null) {
            userKind = -1;
        }
        if (username == null) {
            username = EMPTY_STRING;
        }
        return ZipUtils.zipString(accountId + DELIM + DELIM + kind + DELIM + permission + DELIM + DELIM + userKind + DELIM + username + DELIM + DELIM + DELIM + isSuperAdmin);
    }

    public static BaseJwt decode(String input) {
        BaseJwt result = null;
        try {
            String[] items = Objects.requireNonNull(ZipUtils.unzipString(input)).split(Pattern.quote(DELIM), 6);
            if (items.length >= 6) {
                result = new BaseJwt();
                result.setAccountId(parserLong(items[0]));
                result.setKind(checkString(items[1]));
                result.setPermission(checkString(items[2]));
                result.setUserKind(parserInt(items[3]));
                result.setUsername(checkString(items[4]));
                result.setIsSuperAdmin(checkBoolean(items[5]));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    private static Long parserLong(String input) {
        try {
            long out = Long.parseLong(input);
            if (out > 0) {
                return out;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private static Integer parserInt(String input) {
        try {
            int out = Integer.parseInt(input);
            if (out > 0) {
                return out;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private static String checkString(String input) {
        if (!input.equals(EMPTY_STRING)) {
            return input;
        }
        return null;
    }

    private static Boolean checkBoolean(String input) {
        try {
            return Boolean.parseBoolean(input);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
