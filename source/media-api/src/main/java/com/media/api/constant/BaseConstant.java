package com.media.api.constant;

import java.io.File;

public class BaseConstant {
    public static final String DIRECTORY_TENANT = File.separator + "tenant";
    public static final String DIRECTORY_GENERAL = File.separator + "general";

    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final String NOTIFY_CUSTOMER_QUEUE_NAME = "WS_QUEUE_NOTIFY_ORDER";
    public static final String SESSSION_KEY = "user_sesson";

    public static final Integer TENANT_DB_VERSION = 2;
    public static final String UPDATE_STATUS_PENDING = "0";
    public static final Integer UPDATE_STATUS_UPDATING = 1;
    public static final Integer UPDATE_STATUS_COMPLETED = 2;

    public static final String TOKEN_KIND_SYSTEM = "SYSTEM";
    public static final String TOKEN_KIND_TABLET_QRCODE = "TABLET_QRCODE";
    public static final String TOKEN_KIND_TABLET_BOARD = "TABLET_BOARD";
    public static final String TOKEN_KIND_TABLET_PAYMENT = "TABLET_PAYMENT";
    public static final String TOKEN_KIND_PARTNER_QRCODE = "PARTNER_QRCODE";
    public static final String TOKEN_KIND_MOBILE_QRCODE = "MOBILE_QRCODE";

    public static final String SERVICE_QRCODE = "QRCODE";
    public static final String SERVICE_QRCODE_CUSTOMER = "QRCODE_CUSTOMER"; // khach tu scan qrcode va dat hang tu tablet
    public static final String SERVICE_PICKUP = "PICKUP";
    public static final String SERVICE_DELIVER = "DELIVER";


    public static final Integer USER_KIND_ADMIN = 1;
    public static final Integer USER_KIND_SHOP = 2;
    public static final Integer USER_KIND_CUSTOMER = 3;
    public static final Integer USER_KIND_TABLET = 4;
    public static final Integer USER_KIND_MOBILE = 5;
    public static final Integer USER_KIND_WEBSITE = 6;
    public static final Integer USER_KIND_API_PARTNER = 7;
    public static final Integer USER_KIND_BOARD = 8;
    public static final Integer USER_KIND_PAYMENT = 9;

    public static final Integer STATUS_ACTIVE = 1;
    public static final Integer STATUS_PENDING = 0;
    public static final Integer STATUS_LOCK = -1;
    public static final Integer STATUS_DELETE = -2;

    public static final Integer VIDEO_LIBRARY_STATE_PROCESSING = 0;
    public static final Integer VIDEO_LIBRARY_STATE_READY = 1;
    public static final Integer VIDEO_LIBRARY_STATE_ERROR = 2;

    public static final String CMD_DELETE_FILE = "CMD_DELETE_FILE";
    public static final String CMD_DELETE_TENANT = "CMD_DELETE_TENANT";
    public static final String CMD_DELETE_VIDEO = "CMD_DELETE_VIDEO";
    public static final String CMD_CONVERT_VIDEO = "CMD_CONVERT_VIDEO";
    public static final String CMD_DONE_CONVERT_VIDEO = "CMD_DONE_CONVERT_VIDEO"; // send to tenant to update video library

    private BaseConstant() {
        throw new IllegalStateException("Utility class");
    }
}
