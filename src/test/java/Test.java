import org.apache.log4j.Logger;
import top.wetech.tools.personal.IdCardUtils;

/**
 * company:
 * user: chenzuoli
 * date: 2018/7/18 11:05
 * description: 测试类
 */
public class Test {
    private static Logger logger = Logger.getLogger(Test.class);

    public static void main(String[] args) {
        testIdCardUtils();
    }

    private static void testIdCardUtils() {
        String idCard = "420922921026567";
        String zoomFromIdCard = IdCardUtils.getProvince(idCard);
        System.out.println(zoomFromIdCard);
        String birthday = IdCardUtils.getBirthday(idCard);
        System.out.println(birthday);
        System.out.println(IdCardUtils.getGender(idCard));
        System.out.println(IdCardUtils.getAge(idCard));
    }

}
