package com.cdphantom.suzaku.util;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DateUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

    /** ���ڸ�ʽ��{@value} */
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** ���ڸ�ʽ��{@value} */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /** ���ڸ�ʽ��{@value} */
    public static final String ZH_DATETIME_FORMAT = "yyyy��M��d�� Hʱm��s��";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String ZH_DATE_FORMAT = "yyyy��M��d��";

    /** ���ڸ�ʽ��{@value} */
    public static final String RFC3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String SHORT_DATE_FORMAT = "yyMMdd";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String YYYYMMDD_FORMAT = "yyyyMMdd";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String SECOND_FORMAT = "yyyyMMddHHmmss";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String YYMMDDHHMMSS = "yyMMddHHmmss";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String MILLISECOND_FORMAT = "yyyyMMddHHmmssSSS";
    
    /** ���ڸ�ʽ��{@value} */
    public static final String YYYY_FORMAT = "yyyy";
    
    /**
     * ���ַ���ת���� {@link java.util.Date} ����ʱ��֧�ֵ����ڸ�ʽ��
     *  <ul>
     *  <li>yyyy-MM-dd HH:mm:ss.SSS</li>
     *  <li>yyyy-MM-dd HH:mm:ss</li>
     *  <li>yyyy-MM-dd HH:mm</li>
     *  <li>yyyy-MM-dd</li>
     *  <li>yyyy-MM-dd'T'HH:mm:ss.SSS</li>
     *  <li>yyyy-MM-dd'T'HH:mm:ss</li>
     *  <li>yyyy-MM-dd'T'HH:mm</li>
     *  <li>yyyy/MM/dd HH:mm:ss.SSS</li>
     *  <li>yyyy/MM/dd HH:mm:ss</li>
     *  <li>yyyy/MM/dd HH:mm</li>
     *  <li>yyyy/MM/dd</li>
     *  <li>yyyy��MM��dd�� HHʱmm��ss��</li>
     *  <li>yyyy��MM��dd�� HHʱmm��</li>
     *  <li>yyyy��MM��dd��</li>
     *  <li>yyyyMMddHHmmssSSS</li>
     *  <li>yyyyMMddHHmmss</li>
     *  <li>yyyyMMddHHmm</li>
     *  <li>yyyyMMdd</li>
     *  </ul>
     */
    public static final String[] SUPPORT_FORMATS = new String[] {
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy/MM/dd HH:mm:ss.SSS",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy/MM/dd",
        "yyyy��MM��dd�� HHʱmm��ss��",
        "yyyy��MM��dd�� HHʱmm��",
        "yyyy��MM��dd��",
        "yyyyMMddHHmmssSSS",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm",
        "yyyyMMdd",
        "yyyy"
    };

    /**
     * �����ڸ�ʽ��Ϊ {@value #DEFAULT_DATETIME_FORMAT} ��ʽ���ַ���
     * @param date ��ʽ����������ַ��������ֵΪ {@code null}���򷵻� {@code null}
     * @return ��ʽ����������ַ���
     * @since 2.1.2-SNAPSHOT
     * @see org.apache.commons.lang3.time.DateFormatUtils#format(Date, String)
     */
    public static String format(Date date) {
        return (date == null) ? null : DateFormatUtils.format(date, DEFAULT_DATETIME_FORMAT);
    }

    /**
     * �����ڸ�ʽ��Ϊ {@value #DEFAULT_DATE_FORMAT} ��ʽ���ַ���
     * 
     * @param date
     *            Ҫ��ʽ�������ڶ���ֵΪ <code>null</code> ʱ�����ء��ޡ�
     * @return ��ʽ����������ַ������������� {@code date}
     *         Ϊ <code>null</code>���򣬷��ء��ޡ�
     * @since 2.1.2-SNAPSHOT
     */
    public static String formatWithDefault(Date date) {
        return format(date, DEFAULT_DATE_FORMAT, "��");
    }

    /**
     * ��ʽ������
     * @param date Ҫ��ʽ�������ڶ���Ϊ <code>null</code> ʱ������ {@code defaultValue}
     * @param pattern ���ڸ�ʽ
     * @param defaultValue {@code date} Ϊ {@code null} ʱ���ص�Ĭ��ֵ
     * @return ��ʽ����������ַ������������� {@code date} Ϊ <code>null</code>���򷵻� {@code defaultValue}
     * @since 2.1.2-SNAPSHOT
     * @see org.apache.commons.lang3.time.DateFormatUtils#format(Date, String)
     */
    public static String format(Date date, String pattern, String defaultValue) {
        return (date == null) ? defaultValue : DateFormatUtils.format(date, pattern);
    }
    
    /**
     * �������ַ������������ڶ���
     * @param date �����ַ���������ĸ�ʽΪ��
     * <ul>
     * <li>yyyy-MM-dd HH:mm:ss</li>
     * <li>yyyy-MM-dd</li>
     * <li>yyyy��M��d��H��m��s��</li>
     * <li>yyyy��M��d��</li>
     * <li>yyyy-MM-dd'T'HH:mm:ss</li>
     * <li>yyMMdd</li>
     * <li>yyyyMMdd</li>
     * <li>yyyyMMddHHmmss</li>
     * <li>yyMMddHHmmss</li>
     * <li>yyyyMMddHHmmssSSS"</li>
     * </ul>
     * @return ��Ӧ�����ڶ����������ʧ�ܣ��򷵻� <code>null</code>
     * @see #parse(String, String...)
     * @since 2.1.2-SNAPSHOT
     * @see #parse(String, String...)
     */
    public static Date parse(String date) {
        return parse(date, DEFAULT_DATETIME_FORMAT, DEFAULT_DATE_FORMAT,
                ZH_DATETIME_FORMAT, ZH_DATE_FORMAT, RFC3339_FORMAT,
                SHORT_DATE_FORMAT, YYYYMMDD_FORMAT, SECOND_FORMAT,
                YYMMDDHHMMSS, MILLISECOND_FORMAT);
    }
    
    /**
     * ��������
     * @param date ָ���������ַ���
     * @param format ���ڸ�ʽ������ʹ�ñ����е�һЩ��̬�������� ��<code>DEFAULT_DATETIME_FORMAT</code>
     * @return Date ���ض�Ӧ�����ڶ����������ʧ�ܣ��򷵻� <code>null</code>
     * @see org.apache.commons.lang3.time.DateUtils#parseDate(String, String...)
     */
    public static Date parse(String date, String... format) {
        Date dt = null;
        try {
            dt = DateUtils.parseDate(date, format);
        } catch (ParseException e) {
            LOGGER.warn(e.toString());
        }
        return dt;
    }
    
    /**
     * ȡ�õ��µ�һ��0ʱ0��0��
     * @return ���µ�һ��0ʱ0��0��
     * @since 2.1.2-SNAPSHOT
     * @see #getFirstDayOfMonth(Date)
     */
    public static Date getFirstDayOfMonth() {
        return getFirstDayOfMonth(Calendar.getInstance().getTime());
    }

    /**
     * ȡ��ָ�����������·ݵĵ�һ��0ʱ0��0��
     * @param date ���ڶ���
     * @return {@code date} �����·ݵ�һ��0ʱ0��0��
     * @since 2.1.2-SNAPSHOT
     * @see org.apache.commons.lang3.time.DateUtils#truncate(Date, int)
     */
    public static Date getFirstDayOfMonth(Date date) {
        return DateUtils.truncate(date, Calendar.MONTH);
    }
    
    /**
     * ȡ�õ��µ�һ�쵱�µ�һ��0ʱ0��0����ַ�����ʽ�����صĸ�ʽΪ {@value #DEFAULT_DATETIME_FORMAT}
     * @return ���µ�һ�쵱�µ�һ��0ʱ0��0����ַ�����ʽ
     * @since 2.1.2-SNAPSHOT
     * @see #getFirstDayOfMonthAsString(Date)
     */
    public static String getFirstDayOfMonthAsString() {
        return getFirstDayOfMonthAsString(Calendar.getInstance().getTime());
    }

    /**
     * ȡ��ָ�����������·ݵĵ�һ��0ʱ0��0����ַ�����ʽ�����صĸ�ʽΪ {@value #DEFAULT_DATETIME_FORMAT}
     * @param date ���ڶ���
     * @return {@code date} �����·ݵ�һ��0ʱ0��0����ַ�����ʽ
     * @since 2.1.2-SNAPSHOT
     * @see #getFirstDayOfMonth(Date)
     * @see org.apache.commons.lang3.time.DateFormatUtils#format(Date, String)
     */
    public static String getFirstDayOfMonthAsString(Date date) {
        return DateFormatUtils.format(getFirstDayOfMonth(date), DEFAULT_DATETIME_FORMAT);
    }
    
    /**
     * ȡ�õ������һ��23ʱ59��59��
     * @return �������һ��23ʱ59��59��
     * @since 2.1.2-SNAPSHOT
     * @see #getLastDayOfMonth(Date)
     */
    public static Date getLastDayOfMonth() {
        return getLastDayOfMonth(Calendar.getInstance().getTime());
    }
    
    /**
     * ȡ��ָ�����������·ݵ����һ��23ʱ59��59��
     * @param date ���ڶ���
     * @return �������һ��23ʱ59��59��
     * @since 2.1.2-SNAPSHOT
     * @see org.apache.commons.lang3.time.DateUtils#ceiling(Calendar, int)
     */
    public static Date getLastDayOfMonth(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        
        // ��ȡ�����µ�һ��0ʱ0��0��
        c = DateUtils.ceiling(c, Calendar.MONTH);
        // ��ǰ����һ΢�룬����ȡ���˱������һ��23ʱ59��59��
        c.add(Calendar.MILLISECOND, -1);
        return c.getTime();
    }
    
    /**
     * ȡ�õ������һ��23ʱ59��59����ַ�����ʽ�����صĸ�ʽΪ {@value DEFAULT_DATETIME_FORMAT}
     * @return �������һ��23ʱ59��59����ַ�����ʽ
     * @since 2.1.2-SNAPSHOT
     * @see #getLastDayOfMonthAsString(Date)
     */
    public static String getLastDayOfMonthAsString() {
        return getLastDayOfMonthAsString(Calendar.getInstance().getTime());
    }

    /**
     * ȡ�õ������һ��23ʱ59��59����ַ�����ʽ�����صĸ�ʽΪ {@value DEFAULT_DATETIME_FORMAT}
     * @param date ���ڶ���
     * @return {@code date} �����·����һ��23ʱ59��59����ַ�����ʽ
     * @since 2.1.2-SNAPSHOT
     * @see #getLastDayOfMonth(Date)
     * @see org.apache.commons.lang3.time.DateFormatUtils#format(Date, String)
     */
    public static String getLastDayOfMonthAsString(Date date) {
        return DateFormatUtils.format(getLastDayOfMonth(), DEFAULT_DATETIME_FORMAT);
    }
    
    /**
     *  ���� dateFrom �� dateTo �������
     *  1.ֻ�Ƚ����ڣ�����ʱ����
     *  2.��dateFrom��dateTo֮��ʱ�򷵻ظ���
     *  @param begin ��ʼ����
     *  @param end ��������
     *  @return ��ʼ���ںͽ�������֮�����������
     *  @author xiongyh
     **/
    public static int getDaysBetween(Date begin, Date end) {
        if ((null != begin) && (null != end)) {
            long dateFromTime = DateUtils.truncate(begin, Calendar.DATE).getTime();
            long dateToTime = DateUtils.truncate(end, Calendar.DATE).getTime();
            long dateRange = (dateToTime - dateFromTime) / DateUtils.MILLIS_PER_DAY;
            return (int) dateRange;
        }
        return 0;
    }

    /**
     * ����ת��Ϊ���ָ�ʽ
     * 
     * @author liuliang
     * @param date Ҫ���и�ʽ��������
     * @return �������ڵĺ��ְ棨eg.��Oһ����ʮ�������գ�
     */
    public static String dataToUpper(Date date) {
        if (date == null) {
            return "";
        }
        Calendar ca = Calendar.getInstance();
        ca.setTime(date);
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH) + 1;
        int day = ca.get(Calendar.DAY_OF_MONTH);
        return numToUpper(year) + "��" + monthToUppder(month) + "��" + dayToUppder(day) + "��";
    }

    // ������ת��Ϊ��д
    private static String numToUpper(int num) {
        // String u[] = {"��","Ҽ","��","��","��","��","½","��","��","��"};
        String[] u = { "O", "һ", "��", "��", "��", "��", "��", "��", "��", "��" };
        char[] str = String.valueOf(num).toCharArray();
        String rstr = "";
        for (int i = 0; i < str.length; i++) {
            rstr = rstr + u[Integer.parseInt(str[i] + "")];
        }
        return rstr;
    }

    // ��ת��Ϊ��д
    private static String monthToUppder(int month) {
        if (month < 10) {
            return numToUpper(month);
        } else if (month == 10) {
            return "ʮ";
        } else {
            return "ʮ" + numToUpper(month - 10);
        }
    }

    // ��ת��Ϊ��д
    private static String dayToUppder(int day) {
        if (day < 20) {
            return monthToUppder(day);
        } else {
            char[] str = String.valueOf(day).toCharArray();
            if (str[1] == '0') {
                return numToUpper(Integer.parseInt(str[0] + "")) + "ʮ";
            } else {
                return numToUpper(Integer.parseInt(str[0] + "")) + "ʮ"
                        + numToUpper(Integer.parseInt(str[1] + ""));
            }
        }
    }
}
