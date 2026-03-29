package com.media.api.dto;

public class ErrorCode {
    /**
     * General error code
     */
    public static final String GENERAL_ERROR_REQUIRE_PARAMS = "ERROR-GENERAL-0000";
    public static final String GENERAL_ERROR_RESTAURANT_LOCKED = "ERROR-GENERAL-0001";
    public static final String GENERAL_ERROR_ACCOUNT_LOCKED = "ERROR-GENERAL-0002";
    public static final String GENERAL_ERROR_SHOP_LOCKED = "ERROR-GENERAL-0003";
    public static final String GENERAL_ERROR_RESTAURANT_NOT_FOUND = "ERROR-GENERAL-0004";
    public static final String GENERAL_ERROR_ACCOUNT_NOT_FOUND = "ERROR-GENERAL-0005";

    /**
     * Db config error code
     */
    public static final String DB_CONFIG_ERROR_UNAUTHORIZED = "ERROR-DB-CONFIG-000";
    public static final String DB_CONFIG_ERROR_NOT_FOUND = "ERROR-DB-CONFIG-001";
    public static final String DB_CONFIG_ERROR_NOT_INITIALIZE = "ERROR-DB-CONFIG-002";
    public static final String DB_CONFIG_ERROR_CANNOT_CREATE_DB = "ERROR-DB-CONFIG-003";
    public static final String DB_CONFIG_ERROR_CANNOT_RESTORE_DB = "ERROR-DB-CONFIG-004";
    public static final String DB_CONFIG_ERROR_UPLOAD = "ERROR-DB-RESTORE-005";
    public static final String DB_CONFIG_UPGRADE_TENANT_ALREADY_IN_PROCESS = "ERROR-DB-CONFIG-006";
    public static final String DB_CONFIG_ERROR_DROP = "ERROR-DB-CONFIG-007";
    public static final String DB_CONFIG_SHOP_EXISTED = "ERROR-DB-CONFIG-008";
    /**
     * Starting error code Account
     */
    public static final String ACCOUNT_ERROR_UNKNOWN = "ERROR-ACCOUNT-0000";
    public static final String ACCOUNT_ERROR_USERNAME_EXIST = "ERROR-ACCOUNT-0001";
    public static final String ACCOUNT_ERROR_NOT_FOUND = "ERROR-ACCOUNT-0002";
    public static final String ACCOUNT_ERROR_WRONG_PASSWORD = "ERROR-ACCOUNT-0003";
    public static final String ACCOUNT_ERROR_WRONG_HASH_RESET_PASS = "ERROR-ACCOUNT-0004";
    public static final String ACCOUNT_ERROR_LOCKED = "ERROR-ACCOUNT-0005";
    public static final String ACCOUNT_ERROR_OPT_INVALID = "ERROR-ACCOUNT-0006";
    public static final String ACCOUNT_ERROR_LOGIN = "ERROR-ACCOUNT-0007";
    public static final String ACCOUNT_ERROR_MERCHANT_LOGIN_ERROR_DEVICE = "ERROR-ACCOUNT-0008";
    public static final String ACCOUNT_ERROR_MERCHANT_LOGIN_ERROR_RESTAURANT = "ERROR-ACCOUNT-0009";
    public static final String ACCOUNT_ERROR_MERCHANT_LOGIN_WRONG_RESTAURANT = "ERROR-ACCOUNT-0010";
    public static final String ACCOUNT_ERROR_MERCHANT_SERVICE_NOT_REGISTER = "ERROR-ACCOUNT-0011";


    /**
     * Starting error code AuthenticationToken
     */
    public static final String AUTH_TOKEN_ERROR_UNKNOWN = "ERROR-AUTH-TOKEN-0000";
    public static final String AUTH_TOKEN_ERROR_WRONG_SHOP = "ERROR-AUTH-TOKEN-0001";
    public static final String AUTH_TOKEN_ERROR_NOT_FOUND = "ERROR-AUTH-TOKEN-0002";
    public static final String AUTH_TOKEN_ERROR_INVALID = "ERROR-AUTH-TOKEN-0003";
    public static final String AUTH_TOKEN_ERROR_INVALID_RESTAURANT = "ERROR-AUTH-TOKEN-0004";
    public static final String AUTH_TOKEN_ERROR_INVALID_DEVICE = "ERROR-AUTH-TOKEN-0005";

    /**
     * Starting error code BILLING
     */
    public static final String BILLING_ERROR_UNKNOWN = "ERROR-BILLING-0000";
    public static final String BILLING_ERROR_NOT_FOUND = "ERROR-BILLING-0001";
    public static final String BILLING_ERROR_PAYMENT_ORDER_NOT_FOUND = "ERROR-BILLING-0002";


    /**
     * Starting error code Customer
     */
    public static final String CUSTOMER_ERROR_UNKNOWN = "ERROR-CUSTOMER-0000";
    public static final String CUSTOMER_ERROR_EXIST = "ERROR-CUSTOMER-0002";
    public static final String CUSTOMER_ERROR_UPDATE = "ERROR-CUSTOMER-0003";
    public static final String CUSTOMER_ERROR_NOT_FOUND = "ERROR-CUSTOMER-0004";


    /**
     * Starting error code Employee
     */
    public static final String EMPLOYEE_ERROR_UNKNOWN = "ERROR-EMPLOYEE-0000";
    public static final String EMPLOYEE_ERROR_EXIST = "ERROR-EMPLOYEE-0001";
    public static final String EMPLOYEE_ERROR_NOT_FOUND = "ERROR-EMPLOYEE-0003";
    public static final String EMPLOYEE_ERROR_PASSWORD_DUPLICATE = "ERROR-EMPLOYEE-0004";
    public static final String EMPLOYEE_ERROR_WRONG_RESTAURANT = "ERROR-EMPLOYEE-0005";

    /**
     * Starting error code Food Condition
     */
    public static final String FOOD_CONDITION_ERROR_UNKNOWN = "ERROR-FOOD-CONDITION-0000";
    public static final String FOOD_CONDITION_ERROR_EXIST = "ERROR-FOOD-CONDITION-0001";
    public static final String FOOD_CONDITION_ERROR_NOT_FOUND = "ERROR-FOOD-CONDITION-0002";
    public static final String FOOD_CONDITION_ERROR_PLU_EXIST = "ERROR-FOOD-CONDITION-0003";
    public static final String FOOD_CONDITION_BEILAGE_TEMPLATE_NOT_FOUND = "ERROR-FOOD-CONDITION-0004";
    public static final String FOOD_CONDITION_ERROR_RESTAURANT_CANNOT_NULL = "ERROR-FOOD-CONDITION-0005";

    /**
     * Starting error code Food
     */
    public static final String FOOD_ERROR_UNKNOWN = "ERROR-FOOD-0000";
    public static final String FOOD_ERROR_NOT_FOUND = "ERROR-FOOD-0001";
    public static final String FOOD_ERROR_PLU_EXIST = "ERROR-FOOD-0002";
    public static final String FOOD_ERROR_PLU_CANNOT_NULL = "ERROR-FOOD-0003";
    public static final String FOOD_ERROR_RESTAURANT_CANNOT_NULL = "ERROR-FOOD-0004";
    public static final String FOOD_ERROR_GROUP_FOOD = "ERROR-FOOD-0005";


    /**
     * Starting error code Food Group
     */
    public static final String GROUP_FOOD_ERROR_UNKNOWN = "ERROR-GROUP-FOOD-0000";
    public static final String GROUP_FOOD_ERROR_NOT_FOUND = "ERROR-GROUP-FOOD-0001";
    public static final String GROUP_FOOD_ERROR_RESTAURANT_CANNOT_NULL = "ERROR-GROUP-FOOD-0002";


    /**
     * Starting error code Order
     */
    public static final String ORDER_ERROR_UNKNOWN = "ERROR-ORDER-0000";
    public static final String ORDER_ERROR_NOT_FOUND = "ERROR-ORDER-0001";
    public static final String ORDER_ERROR_VERIFY_QRCODE_QRLIVE_ERROR = "ERROR-ORDER-0002";
    public static final String ORDER_ERROR_SCAN_BY_ANOTHER = "ERROR-ORDER-0004";
    public static final String ORDER_ERROR_QRLIVE_ORDER_EXIST = "ERROR-ORDER-0005";
    public static final String ORDER_ERROR_DATA_FOOD_ERROR = "ERROR-ORDER-0006";
    public static final String ORDER_ERROR_WRONG_EMPLOYEE = "ERROR-ORDER-0007";
    public static final String ORDER_ERROR_WRONG_STATE_DELETE = "ERROR-ORDER-0008";
    public static final String ORDER_ERROR_TABLE_CONTROL_BY_ANOTHER = "ERROR-ORDER-0009";
    public static final String ORDER_ERROR_TABLE_LOCKED_BY_EMPLOYEE = "ERROR-ORDER-0010";
    public static final String ORDER_ERROR_CALL_PAYPAL_ERROR = "ERROR-ORDER-0011";
    // Khi tam ngung phuc vu qrlive
    public static final String ORDER_ERROR_REQUEST_QRCODE_TV_ERROR = "ERROR-ORDER-0012";
    public static final String ORDER_ERROR_CAN_NOT_ORDER_MORE_THAN_ONE_TABLE = "ERROR-ORDER-0013";
    public static final String ORDER_ERROR_NOT_BELONG_TO_ORDER = "ERROR-ORDER-0014";
    public static final String ORDER_ERROR_DATA_TABLE_PARTNER_ERROR = "ERROR-ORDER-0015";
    public static final String ORDER_ERROR_DATA_PEOPLE_PARTNER_ERROR = "ERROR-ORDER-0016";


    /**
     * Starting error code Restaurant
     */
    public static final String RESTAURANT_ERROR_UNKNOWN = "ERROR-RESTAURANT-0000";
    public static final String RESTAURANT_ERROR_NOT_FOUND = "ERROR-RESTAURANT-0001";
    public static final String RESTAURANT_ERROR_DUPLICATE_PATH = "ERROR-RESTAURANT-0002";

    /**
     * Starting error code SHOP ACCOUNT
     */
    public static final String SHOP_ACCOUNT_ERROR_UNKNOWN = "ERROR-SHOP_ACCOUNT-0000";
    public static final String SHOP_ACCOUNT_ERROR_NOT_FOUND = "ERROR-SHOP_ACCOUNT-0001";
    public static final String SHOP_ACCOUNT_ERROR_USERNAME_EXIST = "ERROR-SHOP_ACCOUNT-0002";
    public static final String SHOP_ACCOUNT_ERROR_WRONG_OLD_PWD = "ERROR-SHOP_ACCOUNT-0003";

}
