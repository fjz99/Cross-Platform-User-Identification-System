package edu.nwpu.cpuis.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompressUtilsTest {

    @Test
    void decompressZip() {
        String name = "/E:/123.zip";
        assertTrue (CompressUtils.isEndsWithZip (name));
        assertTrue (CompressUtils.decompressZip (name,"E:/test/"));
    }
}
