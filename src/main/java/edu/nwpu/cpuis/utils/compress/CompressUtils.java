package edu.nwpu.cpuis.utils.compress;

import com.github.junrar.Archive;
import com.github.junrar.UnrarCallback;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.*;
import java.util.List;

/**
 * 只支持zip and rar
 * 抄的
 */
public final class CompressUtils {
    public static void unrar(String rarFileName, String outFilePath, UnrarCallback callback) throws Exception {
        Archive archive = new Archive (new File (rarFileName), callback);
        if (archive == null) {
            throw new FileNotFoundException (rarFileName + " NOT FOUND!");
        }
        if (archive.isEncrypted ()) {
            throw new Exception (rarFileName + " IS ENCRYPTED!");
        }
        List<FileHeader> files = archive.getFileHeaders ();
        for (FileHeader fh : files) {
            if (fh.isEncrypted ()) {
                throw new Exception (rarFileName + " IS ENCRYPTED!");
            }
            String fileName = fh.getFileNameString ();
            if (fileName != null && fileName.trim ().length () > 0) {
                String saveFileName = outFilePath + File.separator + fileName;
                File saveFile = new File (saveFileName);
                File parent = saveFile.getParentFile ();
                if (!parent.exists ()) {
                    parent.mkdirs ();
                }
                if (!saveFile.exists ()) {
                    saveFile.createNewFile ();
                }
                FileOutputStream fos = new FileOutputStream (saveFile);
                try {
                    archive.extractFile (fh, fos);
                } catch (RarException e) {
                    throw e;
                } finally {
                    try {
                        fos.flush ();
                        fos.close ();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * 把文件压缩成zip格式
     *
     * @param files       需要压缩的文件
     * @param zipFilePath 压缩后的zip文件路径   ,如"D:/test/aa.zip";
     */
    public static void compressFiles2Zip(File[] files, String zipFilePath) {
        if (files != null && files.length > 0) {
            if (isEndsWithZip (zipFilePath)) {
                ZipArchiveOutputStream zaos = null;
                try {
                    File zipFile = new File (zipFilePath);
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
                } catch (Exception e) {
                    throw new RuntimeException (e);
                } finally {
                    try {
                        if (zaos != null) {
                            zaos.close ();
                        }
                    } catch (IOException e) {
                        e.printStackTrace ();
                    }
                }

            }

        }

    }

    /**
     * 把zip文件解压到指定的文件夹
     * 目标文件夹必须存在的bug已修复
     *
     * @param zipFilePath zip文件路径, 如 "D:/test/aa.zip"
     * @param saveFileDir 解压后的文件存放路径, 如"D:/test/"
     */
    public static boolean decompressZip(String zipFilePath, String saveFileDir) {
        if (isEndsWithZip (zipFilePath)) {
            File file = new File (zipFilePath);
            if (file.exists ()) {
                File t = new File (saveFileDir);
                if (!t.exists ()) {
                    t.mkdirs ();
                }
                InputStream is = null;
                //can read Zip archives
                ZipArchiveInputStream zais = null;
                try {
                    is = new FileInputStream (file);
                    zais = new ZipArchiveInputStream (is);
                    ArchiveEntry archiveEntry = null;
                    //把zip包中的每个文件读取出来
                    //然后把文件写到指定的文件夹
                    while ((archiveEntry = zais.getNextEntry ()) != null) {
                        //获取文件名
                        String entryFileName = archiveEntry.getName ();
                        //构造解压出来的文件存放路径
                        String entryFilePath = saveFileDir + entryFileName;
                        byte[] content = new byte[(int) archiveEntry.getSize ()];
                        zais.read (content);
                        OutputStream os = null;
                        try {
                            //把解压出来的文件写到指定路径
                            File entryFile = new File (entryFilePath);
//                            if (!entryFile.exists ()) {
//                                entryFile.createNewFile ();
//                            }
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
        } else {
            return false;
        }
    }

    /**
     * 判断文件名是否以.zip为后缀
     *
     * @param fileName 需要判断的文件名
     * @return 是zip文件返回true, 否则返回false
     */
    public static boolean isEndsWithZip(String fileName) {
        boolean flag = false;
        if (fileName != null && !"".equals (fileName.trim ())) {
            if (fileName.endsWith (".ZIP") || fileName.endsWith (".zip")) {
                flag = true;
            }
        }
        return flag;
    }
}
