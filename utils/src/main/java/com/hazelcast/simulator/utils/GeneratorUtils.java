/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.utils;

import java.util.Random;

public final class GeneratorUtils {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz1234567890";
    private static final Random RANDOM = new Random();

    private GeneratorUtils() {
    }

    /**
     * Generates an array of strings.
     *
     * @param count number of String in the array
     * @param length the length of each individual string
     * @return the created array of Strings.
     */
    public static String[] generateStrings(int count, int length) {
        String[] keys = new String[count];
        for (int k = 0; k < keys.length; k++) {
            keys[k] = generateString(length);
        }
        return keys;
    }

    public static String generateString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < length; k++) {
            char c = ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length()));
            sb.append(c);
        }

        return sb.toString();
    }

    public static byte[] generateByteArray(Random random, int length) {
        byte[] result = new byte[length];
        random.nextBytes(result);
        return result;
    }
}
