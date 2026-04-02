/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Profiles {

    public static String PROFILES_PROPERTIES = "profiles.properties";
    
    public static Properties load() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(PROFILES_PROPERTIES)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return props;
    }
    
}
