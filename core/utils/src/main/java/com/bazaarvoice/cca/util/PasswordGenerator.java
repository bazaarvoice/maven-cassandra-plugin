package com.bazaarvoice.cca.util;

public class PasswordGenerator {

    public static String generatePassword(int length) {
        char[] password = new char[length];

        for (int i = 0; i < length; i++) {
            char character = 'a';
            switch ((int) (Math.random() * 2)) {
                case 0: character = getLowercaseLetter(); break;
                case 1: character = getUppercaseLetter(); break;
            }
            password[i] = character;
        }

        // ensure that password is strong enough
        int random = (int) (Math.random() * length);
        password[random] = getSpecialCharacter();
        int random2;
        do {
            random2 = (int) (Math.random() * length);
        } while (random2 == random);
        password[random2] = getNumber();

        return new String(password);
    }

    private static char getLowercaseLetter() {
        return (char) ('a' + (int) (Math.random() * 26));
    }

    private static char getUppercaseLetter() {
        return (char) ('A' + (int) (Math.random() * 26));
    }

    private static char getNumber() {
        return (char) ('0' + (int) (Math.random() * 10));
    }

    private static char getSpecialCharacter() {
        return (char) ('!' + (int) (Math.random() * 15));
    }
}
