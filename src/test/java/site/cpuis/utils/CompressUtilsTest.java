package site.cpuis.utils;

import site.cpuis.utils.compress.CompressUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompressUtilsTest {

    @Test
    void decompressZip() {
        String name = "/E:/123.zip";
        assertTrue (CompressUtils.isEndsWithZip (name));
        assertTrue (CompressUtils.decompressZip (name, "E:/test/"));
    }

    @Test
    void test() throws IOException {
        String srcFile = "C:\\Users\\dell\\Desktop\\fs.zip";
        ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream (new FileInputStream (srcFile), "GBK", true);
        ZipArchiveEntry archiveEntry;
        while ((archiveEntry = zipArchiveInputStream.getNextZipEntry ()) != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream (((int) archiveEntry.getSize ()));
            IOUtils.copy (zipArchiveInputStream, baos);
            System.out.println (baos.toString ("GBK"));
            break;
        }
    }

    @Test
    void testFuck() throws IOException {
        String file = "C:\\Users\\dell\\Desktop\\fs.zip";
        InputStream is = new FileInputStream (file);;
        ZipArchiveInputStream zais = new ZipArchiveInputStream (is);
        ArchiveEntry archiveEntry;
        while ((archiveEntry = zais.getNextEntry ()) != null) {
            byte[] content = new byte[(int) archiveEntry.getSize ()];
            System.out.println (zais.read (content));
            System.out.println (new String (content));
            break;
        }
    }
}
