package org.cpuis.service.validator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;

//验证格式只需要用loader尝试加载数据集即可
@Service
public class DatasetValidator {

    /**
     * @param location 文件夹名
     */
    public FileTypeValidatorOutput validateFileType(String location, String ext) {
        //递归判断所有的文件的扩展名都是ext
        File file = new File (location);
        if (file.exists ()) {
            if (file.isFile () && !checkName (file.getName (), ext)) {
                return new FileTypeValidatorOutput (false, file.getName ());
            }
            File[] files = file.listFiles ();
            if (files == null) {
                return new FileTypeValidatorOutput (true, null);
            }
            for (File listFile : files) {
                FileTypeValidatorOutput output = validateFileType (listFile.getAbsolutePath (), ext);
                if (!output.ok) {
                    return output;
                }
            }
        }
        return new FileTypeValidatorOutput (true, null);
    }

    private boolean checkName(String name, String ext) {
        if (!name.contains (".")) {
            return false;
        }
        return name.substring (name.lastIndexOf ('.') + 1).equals (ext);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileTypeValidatorOutput {
        private boolean ok;
        private String failedFile;
    }
}
