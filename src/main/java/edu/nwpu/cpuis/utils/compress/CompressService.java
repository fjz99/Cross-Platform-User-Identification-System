package edu.nwpu.cpuis.utils.compress;

import java.io.File;
import java.util.List;

public interface CompressService {
    boolean support(String fileName);

    boolean decompress(File file, String path);

    /**
     *
     * @param files
     * @param path 输出压缩文件名,如 E:/123.zip
     * @return
     */
    boolean compress(List<File> files, String path);
}
