package edu.nwpu.cpuis.utils;


public final class ModelKeyGenerator {
    /**
     * @param phase 即predict、train、test等
     * @param type  即元数据还是输出
     */
    public static String generateKey(String[] dataset, String algoName, String phase, String type) {
        StringBuilder stringBuilder = new StringBuilder ();
        if (algoName != null)
            stringBuilder.append (algoName).append ('-');
        if (dataset != null) {
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
}
