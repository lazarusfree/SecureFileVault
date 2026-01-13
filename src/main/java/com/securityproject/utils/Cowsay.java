package com.securityproject.utils;

public class Cowsay {

    public static String say(String message) {
        int length = message.length();
        StringBuilder sb = new StringBuilder();

        // Top border
        sb.append(" ");
        for (int i = 0; i < length + 2; i++) {
            sb.append("_");
        }
        sb.append(" \n");

        // Message
        sb.append("< " + message + " >\n");

        // Bottom border
        sb.append(" ");
        for (int i = 0; i < length + 2; i++) {
            sb.append("-");
        }
        sb.append(" \n");

        // The Cow
        sb.append("        \\   ^__^\n");
        sb.append("         \\  (oo)\\_______\n");
        sb.append("            (__)\\       )\\/\\\n");
        sb.append("                ||----w |\n");
        sb.append("                ||     ||\n");

        return sb.toString();
    }
}
