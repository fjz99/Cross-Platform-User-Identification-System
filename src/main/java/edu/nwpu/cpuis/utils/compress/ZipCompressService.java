package edu.nwpu.cpuis.utils.compress;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;

/**
 * @author fujiazheng
 */
@Component
public class ZipCompressService implements CompressService, Ordered {
    @Override
    public boolean support(String fileName) {
        boolean flag = false;
        if (fileName != null && !"".equals (fileName.trim ())) {
            if (fileName.endsWith (".ZIP") || fileName.endsWith (".zip")) {
                flag = true;
            }
        }
        return flag;
    }

    @Override
    public boolean decompress(File file, String path) {
        if (file.exists ()) {
            InputStream is = null;
            //can read Zip archives
            ZipArchiveInputStream zais = null;
            try {
                is = new FileInputStream (file);
                zais = new ZipArchiveInputStream (is);
                ArchiveEntry archiveEntry;
                //把zip包中的每个文件读取出来
                //然后把文件写到指定的文件夹
                while ((archiveEntry = zais.getNextEntry ()) != null) {
                    //获取文件名
                    String entryFileName = archiveEntry.getName ();
                    //构造解压出来的文件存放路径
                    String entryFilePath = path + entryFileName;
                    byte[] content = new byte[(int) archiveEntry.getSize ()];
                    zais.read (content);
                    OutputStream os = null;
                    try {
                        //把解压出来的文件写到指定路径
                        File entryFile = new File (entryFilePath);
                        os = new BufferedOutputStream (new FileOutputStream (entryFile));
                        os.write (content);
                    } catch (IOException e) {
                        throw new IOException (e);
                    } finally {
                        if (os != null) {
                            os.flush ();
                            os.close ();
                        }
                    }

                }
                return true;
            } catch (Exception e) {
                e.printStackTrace ();
                return false;
            } finally {
                try {
                    if (zais != null) {
                        zais.close ();
                    }
                    if (is != null) {
                        is.close ();
                    }
                } catch (IOException e) {
                    e.printStackTrace ();
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean compress(List<File> files, String path) {
        if (files != null && files.size () > 0) {
            ZipArchiveOutputStream zaos = null;
            try {
                File zipFile = new File (path);
                zaos = new ZipArchiveOutputStream (zipFile);
                //Use Zip64 extensions for all entries where they are required
                zaos.setUseZip64 (Zip64Mode.AsNeeded);

                //将每个文件用ZipArchiveEntry封装
                //再用ZipArchiveOutputStream写到压缩文件中
                for (File file : files) {
                    if (file != null) {
                        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry (file, file.getName ());
                        zaos.putArchiveEntry (zipArchiveEntry);
                        try (InputStream is = new BufferedInputStream (new FileInputStream (file))) {
                            byte[] buffer = new byte[1024 * 5];
                            int len = -1;
                            while ((len = is.read (buffer)) != -1) {
                                //把缓冲区的字节写入到ZipArchiveEntry
                                zaos.write (buffer, 0, len);
                            }
                            //Writes all necessary data for this entry.
                            zaos.closeArchiveEntry ();
                        } catch (Exception e) {
                            throw new RuntimeException (e);
                        }

                    }
                }
                zaos.finish ();
                return true;
            } catch (Exception e) {
                e.printStackTrace ();
                return false;
            } finally {
                try {
                    if (zaos != null) {
                        zaos.close ();
                    }
                } catch (IOException e) {
                    e.printStackTrace ();
                }
            }
        } else return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
