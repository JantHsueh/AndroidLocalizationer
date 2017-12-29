package module;

import com.sargeraswang.util.ExcelUtil.ExcelCell;

public class ExcelString {


    /**
     * xml 中 string 键值对的key
     */
    @ExcelCell(index = 0)
    private String a;
    /**
     * 中文xml中的string 键值对的值
     */
    @ExcelCell(index = 1)
    private String b;
    /**
     * 对应翻译语言xml中的string 键值对的值
     */
    @ExcelCell(index = 2)
    private String c;


    public ExcelString(String a, String b, String c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * @return the a
     */
    public String getA() {
        return a;
    }

    /**
     * @param a the a to set
     */
    public void setA(String a) {
        this.a = a;
    }

    /**
     * @return the b
     */
    public String getB() {
        return b;
    }

    /**
     * @param b the b to set
     */
    public void setB(String b) {
        this.b = b;
    }

    /**
     * @return the c
     */
    public String getC() {
        return c;
    }

    /**
     * @param c the c to set
     */
    public void setC(String c) {
        this.c = c;
    }

}