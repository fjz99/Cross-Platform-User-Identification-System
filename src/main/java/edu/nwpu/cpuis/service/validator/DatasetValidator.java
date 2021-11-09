package edu.nwpu.cpuis.service.validator;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DatasetValidator {
    /**
     * @param location 文件夹名
     * @return
     */
    public boolean validateFileType(String location, String ext) {
        //递归判断所有的文件的扩展名都是ext
        File file = new File (location);
        if (file.exists ()) {
            if (file.isFile () && !checkName (file.getName (), ext)) {
                return false;
            }
            File[] files = file.listFiles ();
            if (files == null) {
                return true;
            }
            for (File listFile : files) {
                if (!validateFileType (listFile.getAbsolutePath (), ext)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkName(String name, String ext) {
        if (!name.contains (".")) {
            return false;
        }
        return name.substring (name.lastIndexOf ('.') + 1).equals (ext);
    }

//    todo
//    public boolean validateFormat() {
//
//    }
}
