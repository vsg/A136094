/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static final int MASK_18 = (1 << 18) - 1;
    public static final int MASK_9 = (1 << 9) - 1;
    public static final int MASK_4 = (1 << 4) - 1;

    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static String formatLog(String format, Object... args) {
        String date = LOG_DATE_FORMAT.format(new Date());
        long usedHeapMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        String message = String.format(format, args);
        return String.format("%s [%d MB] - %s", date, usedHeapMB, message);
    }

    public static String timeStr(long millis) {
        StringBuilder builder = new StringBuilder();
        String suffix = "dhms";
        long[] divider = new long[] {TimeUnit.DAYS.toMillis(1), TimeUnit.HOURS.toMillis(1), 
                TimeUnit.MINUTES.toMillis(1), TimeUnit.SECONDS.toMillis(1)};
        for (int i = 0; i < suffix.length(); i++) {
            if (builder.length() > 0 || millis >= divider[i] || i+1 == suffix.length()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                long value = millis / divider[i];
                builder.append(value);
                builder.append(suffix.charAt(i));
                millis -= value * divider[i];
            }
        }
        return builder.toString();
    }

}
