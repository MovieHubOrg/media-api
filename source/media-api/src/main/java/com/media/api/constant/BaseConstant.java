package com.media.api.constant;

import java.io.File;

public class BaseConstant {
    public static final String DIRECTORY_TENANT = File.separator + "tenant";
    public static final String DIRECTORY_GENERAL = File.separator + "general";
    public static final String DIRECTORY_LIBRARY = "LIBRARY";

    public static final String AUDIO_FILE_NAME = "audio.wav";

    public static final Integer USER_KIND_ADMIN = 1;
    public static final Integer USER_KIND_CUSTOMER = 3;

    public static final Integer STATUS_ACTIVE = 1;

    public static final Integer VIDEO_LIBRARY_STATE_PROCESSING = 0;
    public static final Integer VIDEO_LIBRARY_STATE_READY = 1;
    public static final Integer VIDEO_LIBRARY_STATE_ERROR = 2;

    public static final String REASON_DOWNLOAD_TEMP_FILE_FAILED = "download_temp_file_failed";
    public static final String REASON_CONVERT_FILE_FAILED = "convert_file_failed";
    public static final String REASON_GENERATE_VTT_FAILED = "generate_vtt_failed";
    public static final String REASON_CONVERT_AUDIO_FAILED = "convert_audio_failed";
    public static final String REASON_DOWNLOAD_VTT_FILE_FAILED = "download_vtt_file_failed";

    public static final String CMD_DELETE_VIDEO = "CMD_DELETE_VIDEO";
    public static final String CMD_DELETE_SUBTITLE = "CMD_DELETE_SUBTITLE";
    public static final String CMD_CONVERT_VIDEO = "CMD_CONVERT_VIDEO";
    public static final String CMD_CONVERT_AUDIO = "CMD_CONVERT_AUDIO";
    public static final String CMD_DONE_CONVERT_VIDEO = "CMD_DONE_CONVERT_VIDEO"; // send to tenant to update video library
    public static final String CMD_DONE_CONVERT_AUDIO = "CMD_DONE_CONVERT_AUDIO"; // send to tenant to update audio url
    public static final String CMD_PROCESS_SUBTITLE = "CMD_PROCESS_SUBTITLE";     // send to subtitle service to generate VTT
    public static final String CMD_DONE_PROCESS_SUBTITLE = "CMD_DONE_PROCESS_SUBTITLE";
    public static final String CMD_TRANSLATE_SUBTITLE = "CMD_TRANSLATE_SUBTITLE";
    public static final String CMD_DONE_TRANSLATE_SUBTITLE = "CMD_DONE_TRANSLATE_SUBTITLE";
    public static final String CMD_UPDATE_SERVER_CONFIG = "CMD_UPDATE_SERVER_CONFIG";

    private BaseConstant() {
        throw new IllegalStateException("Utility class");
    }
}
