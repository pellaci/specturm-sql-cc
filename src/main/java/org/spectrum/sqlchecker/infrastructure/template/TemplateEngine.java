package org.spectrum.sqlchecker.infrastructure.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Pebble 模板引擎包装器
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
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
                .cacheActive(true)
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
}
