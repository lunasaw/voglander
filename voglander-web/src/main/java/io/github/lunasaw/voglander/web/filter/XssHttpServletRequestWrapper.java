package io.github.lunasaw.voglander.web.filter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * XSS过滤处理
 *
 * @author luna
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper
{
    /**
     * @param request
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    @Override
    public String[] getParameterValues(String name)
    {
        String[] values = super.getParameterValues(name);
        if (values != null)
        {
            int length = values.length;
            String[] escapesValues = new String[length];
            for (int i = 0; i < length; i++)
            {
                // 防xss攻击和过滤前后空格
                escapesValues[i] = StringEscapeUtils.escapeHtml4(values[i]).trim();
            }
            return escapesValues;
        }
        return super.getParameterValues(name);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        // 非json类型，直接返回
        if (!isJsonRequest())
        {
            return super.getInputStream();
        }

        // 为空，直接返回
        String json = IOUtils.toString(super.getInputStream(), StandardCharsets.UTF_8);
        if (StringUtils.isEmpty(json))
        {
            return super.getInputStream();
        }

        // 对于JSON请求，不进行HTML转义，避免破坏JSON格式
        // 仅进行基本的XSS字符清理，保持JSON结构完整
        json = cleanXssForJson(json).trim();
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        final ByteArrayInputStream bis = new ByteArrayInputStream(jsonBytes);
        return new ServletInputStream()
        {
            @Override
            public boolean isFinished()
            {
                return true;
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public int available() throws IOException
            {
                return jsonBytes.length;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
            }

            @Override
            public int read() throws IOException
            {
                return bis.read();
            }
        };
    }

    /**
     * 针对JSON内容进行XSS清理，保持JSON格式不被破坏
     *
     * @param json JSON字符串
     * @return 清理后的JSON字符串
     */
    private String cleanXssForJson(String json) {
        if (StringUtils.isEmpty(json)) {
            return json;
        }

        // 移除潜在的脚本标签和危险字符，但保持JSON格式
        return json.replaceAll("(?i)<script[^>]*>.*?</script>", "")
            .replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "")
            .replaceAll("(?i)<object[^>]*>.*?</object>", "")
            .replaceAll("(?i)<embed[^>]*>.*?</embed>", "")
            .replaceAll("(?i)javascript:", "")
            .replaceAll("(?i)vbscript:", "")
            .replaceAll("(?i)onload=", "")
            .replaceAll("(?i)onerror=", "")
            .replaceAll("(?i)onclick=", "");
    }

    /**
     * 是否是Json请求
     *
     */
    public boolean isJsonRequest()
    {
        String header = super.getHeader(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }
}