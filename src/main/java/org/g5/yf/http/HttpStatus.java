package org.g5.yf.http;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HttpStatus {
            CONTINUE (100, "CONTINUE"),
            SWITCHING_PROTOCOLS (101, "SWITCHING PROTOCOLS"),
            PROCESSING (102, "PROCESSING"),
            EARLY_HINTS (103, "EARLY HINTS"),
            OK (200, "OK"),
            CREATED (201, "CREATED"),
            ACCEPTED (202, "ACCEPTED"),
            NON_AUTHORITATIVE_INFORMATION (203, "NON-AUTHORITATIVE INFORMATION"),
            NO_CONTENT (204, "NO CONTENT"),
            RESET_CONTENT (205, "RESET CONTENT"),
            PARTIAL_CONTENT (206, "PARTIAL CONTENT"),
            MULTI_STATUS (207, "MULTI-STATUS"),
            ALREADY_REPORTED (208, "ALREADY REPORTED"),
            IM_USED (226, "IM USED"),
            MULTIPLE_CHOICES (300, "MULTIPLE CHOICES"),
            MOVED_PERMANENTLY (301, "MOVED PERMANENTLY"),
            FOUND (302, "FOUND"),
            SEE_OTHER (303, "SEE OTHER"),
            NOT_MODIFIED (304, "NOT MODIFIED"),
            TEMPORARY_REDIRECT (307, "TEMPORARY REDIRECT"),
            PERMANENT_REDIRECT (308, "PERMANENT REDIRECT"),
            BAD_REQUEST (400, "BAD REQUEST"),
            UNAUTHORIZED (401, "UNAUTHORIZED"),
            PAYMENT_REQUIRED (402, "PAYMENT REQUIRED"),
            FORBIDDEN (403, "FORBIDDEN"),
            NOT_FOUND (404, "NOT FOUND"),
            METHOD_NOT_ALLOWED (405, "METHOD NOT ALLOWED"),
            NOT_ACCEPTABLE (406, "NOT ACCEPTABLE"),
            PROXY_AUTHENTICATION_REQUIRED (407, "PROXY AUTHENTICATION REQUIRED"),
            REQUEST_TIMEOUT (408, "REQUEST TIMEOUT"),
            CONFLICT_(409, "CONFLICT"),
            GONE (410, "GONE"),
            LENGTH_REQUIRED (411, "LENGTH REQUIRED"),
            PRECONDITION_FAILED (412, "PRECONDITION FAILED"),
            CONTENT_TOO_LARGE (413, "CONTENT TOO LARGE"),
            URI_TOO_LONG (414, "URI TOO LONG"),
            UNSUPPORTED_MEDIA_TYPE (415, "UNSUPPORTED MEDIA TYPE"),
            RANGE_NOT_SATISFIABLE (416, "RANGE NOT SATISFIABLE"),
            EXPECTATION_FAILED (417, "EXPECTATION FAILED"),
            IM_A_TEAPOT (418, "I'M A TEAPOT"),
            MISDIRECTED_REQUEST (421, "MISDIRECTED REQUEST"),
            UNPROCESSABLE_CONTENT (422, "UNPROCESSABLE CONTENT"),
            LOCKED (423, "LOCKED"),
            FAILED_DEPENDENCY (424, "FAILED DEPENDENCY"),
            TOO_EARLY (425, "TOO EARLY"),
            UPGRADE_REQUIRED (426, "UPGRADE REQUIRED"),
            PRECONDITION_REQUIRED (428, "PRECONDITION REQUIRED"),
            TOO_MANY_REQUESTS (429, "TOO MANY REQUESTS"),
            REQUEST_HEADER_FIELDS_TOO_LARGE (431, "REQUEST HEADER FIELDS TOO LARGE"),
            UNAVAILABLE_FOR_LEGAL_REASONS (451, "UNAVAILABLE FOR LEGAL REASONS"),
            INTERNAL_SERVER_ERROR (500, "INTERNAL SERVER ERROR"),
            NOT_IMPLEMENTED (501, "NOT IMPLEMENTED"),
            BAD_GATEWAY (502, "BAD GATEWAY"),
            SERVICE_UNAVAILABLE (503, "SERVICE UNAVAILABLE"),
            GATEWAY_TIMEOUT (504, "GATEWAY TIMEOUT"),
            HTTP_VERSION_NOT_SUPPORTED (505, "HTTP VERSION NOT SUPPORTED"),
            VARIANT_ALSO_NEGOTIATES (506, "VARIANT ALSO NEGOTIATES"),
            INSUFFICIENT_STORAGE (507, "INSUFFICIENT STORAGE"),
            LOOP_DETECTED (508, "LOOP DETECTED"),
            NOT_EXTENDED (510, "NOT EXTENDED");

    private static final Map<Integer, HttpStatus> codeStatusMap =
            Arrays.stream(HttpStatus.values()).collect(Collectors.toMap(HttpStatus::code, Function.identity()));

    private final int code;
    private final String status;

    HttpStatus(int code, String status) {
        this.code = code;
        this.status = status;
    }

    public int code() {
        return code;
    }

    public String status() {
        return status;
    }

    public boolean isOkStatus() {
        return (this.code >= 200 && this.code < 300);
    }

    public static HttpStatus resolve(int code) {
        return codeStatusMap.getOrDefault(code, HttpStatus.NOT_IMPLEMENTED);
    }
}
