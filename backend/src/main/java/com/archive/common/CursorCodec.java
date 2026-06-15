package com.archive.common;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 游标编解码：cursor = Base64( URLSafe( "ISO时间戳|id" ) )。
 * 游标内含 (时间戳, id) 复合键，配合按 (时间戳 DESC, id DESC) 排序实现稳定翻页。
 */
public final class CursorCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {}

    public static String encode(OffsetDateTime ts, Long id) {
        String raw = ts.toString() + "|" + id;
        return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 解码失败返回 null（非法/空 cursor） */
    public static Decoded decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(DECODER.decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.lastIndexOf('|');
            if (sep < 0) return null;
            OffsetDateTime ts = OffsetDateTime.parse(raw.substring(0, sep));
            Long id = Long.parseLong(raw.substring(sep + 1));
            return new Decoded(ts, id);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return null;
        }
    }

    public record Decoded(OffsetDateTime timestamp, Long id) {}
}
