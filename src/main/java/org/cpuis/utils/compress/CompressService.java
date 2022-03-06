package org.cpuis.utils.compress;

import java.io.File;
import java.util.List;

public interface CompressService {
    boolean support(String fileName);

    boolean decompress(File file, String path);

    /**
     * @param path 输出压缩文件名,如 E:/123.zip
     */
    boolean compress(List<File> files, String path);
}
