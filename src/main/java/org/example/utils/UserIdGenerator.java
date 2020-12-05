package org.example.utils;

import org.apache.commons.lang3.RandomUtils;

/**
 * Created by daqwang on 28/11/20.
 */

public class UserIdGenerator {

    public UserIdGenerator(){}
    public static String generateUserId(){
        return String.valueOf(RandomUtils.nextInt(100, 999));
    }
}
