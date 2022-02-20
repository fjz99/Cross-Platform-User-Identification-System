package edu.nwpu.cpuis.utils;


import java.util.Arrays;

public final class ModelKeyGenerator {

    private ModelKeyGenerator() {

    }

    /**
     * @param dataset 会自动排序
     * @param phase   即predict、getDaemon、test等
     * @param type    即统计信息还是输出
     */
    public static String generateKey(String[] dataset, String algoName, String phase, String type) {
        StringBuilder stringBuilder = new StringBuilder ();
        if (algoName != null)
            stringBuilder.append (algoName).append ('-');
        if (dataset != null) {
            Arrays.sort (dataset);
            for (String s : dataset) {
                stringBuilder.append (s).append ('-');
            }
        }
        if (phase != null) {
            stringBuilder.append (phase).append ('-');
        }
        if (type != null) {
            stringBuilder.append (type);
        }
        int lastIndex = stringBuilder.length () - 1;
        if (stringBuilder.charAt (lastIndex) == '-') {
            stringBuilder.deleteCharAt (lastIndex);
        }
        return stringBuilder.toString ();
    }

    //mongoDB also use this id.
    public static String generateKeyWithIncId(String[] dataset, String algoName, String phase, String type, int thisId) {
        return generateKey (dataset, algoName, phase, type) + "-" + thisId;
    }

    public static String generateModelInfoKey(String[] dataset, String algoName, String phase, String type, String prefix) {
        return prefix + ":" + generateKey (dataset, algoName, phase, type);
    }

    /**
     * 特殊的方法，隔离不同trainWrapper的statistics collection
     */
    public static String generateKey0(String[] dataset, String algoName, String phase, String type) {
        return generateKey (dataset, algoName, phase, type) + "TRACED_WRAPPER";
    }
}
