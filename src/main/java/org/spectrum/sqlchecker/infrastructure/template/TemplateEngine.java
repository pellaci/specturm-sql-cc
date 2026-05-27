package org.spectrum.sqlchecker.infrastructure.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Pebble 模板引擎包装器
 *
 * @author Spectrum SQL Checker
 * @since 2.0.0
 */
@Slf4j
@Component
public class TemplateEngine {

    private final PebbleEngine engine;

    public TemplateEngine() {
        ClasspathLoader loader = new ClasspathLoader();
        loader.setPrefix("templates");
        loader.setSuffix(".pebble");

        this.engine = new PebbleEngine.Builder()
                .loader(loader)
                .strictVariables(false)
                .autoEscaping(true)
                .defaultEscapingStrategy("html")
                .cacheActive(false)  // 开发环境禁用缓存
                .extension(new NumberFormatExtension())
                .build();
    }

    /**
     * 渲染模板
     *
     * @param templateName 模板名称
     * @param context 模板上下文
     * @return 渲染结果
     */
    public String render(String templateName, Map<String, Object> context) {
        try {
            Writer writer = new StringWriter();
            engine.getTemplate(templateName).evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            log.error("Failed to render template: {}", templateName, e);
            throw new RuntimeException("Template rendering failed: " + templateName, e);
        }
    }

    /**
     * 渲染模板到 Writer
     *
     * @param templateName 模板名称
     * @param context 模板上下文
     * @param writer 输出 Writer
     */
    public void render(String templateName, Map<String, Object> context, Writer writer) {
        try {
            engine.getTemplate(templateName).evaluate(writer, context);
        } catch (IOException e) {
            log.error("Failed to render template: {}", templateName, e);
            throw new RuntimeException("Template rendering failed: " + templateName, e);
        }
    }

    /**
     * 数字格式化扩展
     */
    private static class NumberFormatExtension extends AbstractExtension {
        @Override
        public Map<String, Filter> getFilters() {
            return Map.of("numberFormat", new NumberFormatFilter());
        }
    }

    /**
     * 数字格式化过滤器
     */
    private static class NumberFormatFilter implements Filter {
        @Override
        public List<String> getArgumentNames() {
            return null;
        }

        @Override
        public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
            if (input == null) {
                return "0";
            }
            if (input instanceof Number) {
                Number number = (Number) input;
                // 格式化为整数或保留一位小数
                if (number instanceof Integer || number instanceof Long) {
                    return String.valueOf(number.longValue());
                } else {
                    DecimalFormat df = new DecimalFormat("#.#");
                    return df.format(number.doubleValue());
                }
            }
            return String.valueOf(input);
        }
    }
}
