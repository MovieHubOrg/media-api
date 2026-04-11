package com.media.api.constant;

import java.io.File;

public class BaseConstant {
    public static final String DIRECTORY_TENANT = File.separator + "tenant";
    public static final String DIRECTORY_GENERAL = File.separator + "general";

    public static final Integer USER_KIND_ADMIN = 1;
    public static final Integer USER_KIND_CUSTOMER = 3;

    public static final Integer STATUS_ACTIVE = 1;

    public static final Integer VIDEO_LIBRARY_STATE_PROCESSING = 0;
    public static final Integer VIDEO_LIBRARY_STATE_READY = 1;
    public static final Integer VIDEO_LIBRARY_STATE_ERROR = 2;

    public static final String CMD_DELETE_VIDEO = "CMD_DELETE_VIDEO";
    public static final String CMD_CONVERT_VIDEO = "CMD_CONVERT_VIDEO";
    public static final String CMD_DONE_CONVERT_VIDEO = "CMD_DONE_CONVERT_VIDEO"; // send to tenant to update video library
    public static final String CMD_UPDATE_SERVER_CONFIG = "CMD_UPDATE_SERVER_CONFIG";

    private BaseConstant() {
        throw new IllegalStateException("Utility class");
    }
}
