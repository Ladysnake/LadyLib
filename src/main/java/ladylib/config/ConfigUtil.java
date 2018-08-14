package ladylib.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigUtil {
    private ConfigUtil() { }

    private static final Pattern WILDCARD_PATTERN = Pattern.compile("(.*?)\\*");

    public static Pattern wildcardToRegex(String wildcard) {
        return wildcardToRegex(wildcard, true);
    }

    public static Pattern wildcardToRegex(String wildcard, boolean allowRawRegex) {
        if (allowRawRegex && wildcard.charAt(0) == '/' && wildcard.charAt(wildcard.length()-1) == '/') {
            return Pattern.compile(wildcard.substring(1, wildcard.length()-1));
        }
        Matcher m = WILDCARD_PATTERN.matcher(wildcard);
        String regex;
        if (m.find()) {
            regex = m.replaceAll("$1\\\\E\\\\w*\\\\Q");
        } else {
            regex = wildcard;
        }
        regex = "\\Q" + regex + "\\E";
        return Pattern.compile(regex);
    }

}
