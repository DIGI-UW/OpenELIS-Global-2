package org.openelisglobal.common.provider.validation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IntegerUtil;

public class TemplateTokenEngine {

    public static String processTokens(String template, long sequence) {
        String prefix = "";
        try {
            prefix = ConfigurationProperties.getInstance().getPropertyValue(Property.ALPHANUM_ACCESSION_PREFIX);
        } catch (Exception e) {
            // Spring context not available, likely in unit test
        }
        return processTokens(template, sequence, prefix);
    }

    public static String processTokens(String template, long sequence, String prefix) {
        if (template == null)
            return "";

        String result = template;
        Date now = new Date();

        // Standard time tokens
        result = result.replace("{YYYY}", new SimpleDateFormat("yyyy").format(now));
        result = result.replace("{YY}", new SimpleDateFormat("yy").format(now));
        result = result.replace("{MM}", new SimpleDateFormat("MM").format(now));
        result = result.replace("{DD}", new SimpleDateFormat("dd").format(now));

        // Prefix token
        result = result.replace("{PREFIX}", prefix != null ? prefix : "");

        // Sequence tokens
        Pattern seqPattern = Pattern.compile("\\{SEQ:(\\d+)\\}");
        Matcher seqMatcher = seqPattern.matcher(result);
        while (seqMatcher.find()) {
            int n = Integer.parseInt(seqMatcher.group(1));
            result = result.replace(seqMatcher.group(0), StringUtils.leftPad(String.valueOf(sequence), n, '0'));
        }

        Pattern alphaPattern = Pattern.compile("\\{ALPHANUMSEQ:(\\d+)\\}");
        Matcher alphaMatcher = alphaPattern.matcher(result);
        while (alphaMatcher.find()) {
            int n = Integer.parseInt(alphaMatcher.group(1));
            result = result.replace(alphaMatcher.group(0),
                    StringUtils.leftPad(IntegerUtil.toStringBase27((int) sequence), n, '0'));
        }

        return result;
    }

    public static String getStaticPrefix(String template) {
        if (template == null)
            return "";

        // Replace sequence tokens with empty for calculating prefix
        String tempTemplate = template.replaceAll("\\{SEQ:\\d+\\}", "").replaceAll("\\{ALPHANUMSEQ:\\d+\\}", "");

        return processTokens(tempTemplate, 0);
    }
}
