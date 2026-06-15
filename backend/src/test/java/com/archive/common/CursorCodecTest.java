package com.archive.common;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CursorCodecTest {

    @Test
    void 编解码往返一致() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC);
        String cursor = CursorCodec.encode(ts, 123L);
        CursorCodec.Decoded d = CursorCodec.decode(cursor);
        assertThat(d).isNotNull();
        assertThat(d.id()).isEqualTo(123L);
        assertThat(d.timestamp()).isEqualTo(ts);
    }

    @Test
    void decode非法cursor返回null() {
        assertThat(CursorCodec.decode("!!!不是合法base64!!!")).isNull();
        assertThat(CursorCodec.decode(null)).isNull();
        assertThat(CursorCodec.decode("")).isNull();
    }
}
