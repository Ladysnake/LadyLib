package ladylib.config;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigUtilTest {

    @Test
    public void wildcardToRegex() {
        String test = "*ind";
        String valid = "grind";
        String invalid = "fund";
        Pattern pattern = ConfigUtil.wildcardToRegex(test);
        assertTrue(pattern.matcher(valid).matches());
        assertFalse(pattern.matcher(invalid).matches());
    }

    @Test
    public void wildcardRawRegex() {
        String wildcard = "abc.?";
        String regex = "/abc.?/";
        assertFalse(ConfigUtil.wildcardToRegex(wildcard).matcher("abcd").matches());
        assertTrue(ConfigUtil.wildcardToRegex(regex).matcher("abcd").matches());
        assertFalse(ConfigUtil.wildcardToRegex(regex, false).matcher("abcd").matches());
    }
}