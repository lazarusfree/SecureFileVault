package com.securityproject.util;

public final class Cowsay {
    private Cowsay() {}

    public static String say(String message) {
        int length = message.length();
        StringBuilder sb = new StringBuilder();

        sb.append(" ");
        sb.append("_".repeat(length + 2));
        sb.append(" \n");

        sb.append("< ").append(message).append(" >\n");

        sb.append(" ");
        sb.append("-".repeat(length + 2));
        sb.append(" \n");

        sb.append("        \\   ^__^\n");
        sb.append("         \\  (oo)\\_______\n");
        sb.append("            (__)\\       )\\/\\\n");
        sb.append("                ||----w |\n");
        sb.append("                ||     ||\n");

        return sb.toString();
    }
}
