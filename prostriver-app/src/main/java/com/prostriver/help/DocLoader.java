package com.prostriver.help;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the help documentation from the classpath once, at construction, and holds it in memory.
 *
 * <p>Each file is markdown opening with a YAML front matter block that carries {@code title},
 * {@code slug}, {@code summary} and a {@code questions} list of user phrasings. Everything after
 * the front matter is the body.
 *
 * <p>This loader never stops the application from starting. A file that will not parse is logged
 * and skipped on its own, and the remaining documents still load. Finding nothing at all is logged
 * as an error and leaves an empty index. A documentation problem degrades the help endpoint, it
 * does not take the API down with it.
 *
 * <p>Because failures are silent to the caller, they are counted into the
 * {@code prostriver.help.documents.*} gauges. Alert on
 * {@code prostriver_help_documents_skipped > 0}, since a skipped document still has its phrasings
 * sitting in the index and will retrieve as a slug with no body behind it.
 */
@Component
@Profile("api")
public class DocLoader {

    static final String DEFAULT_LOCATION = "classpath*:help-docs/*.md";

    private static final Logger log = LoggerFactory.getLogger(DocLoader.class);

    /**
     * Front matter is the block between the opening {@code ---} and the next {@code ---} on its own
     * line. The lazy group stops at the first closing delimiter, so a {@code ---} horizontal rule
     * later in the body is left alone.
     */
    private static final Pattern FRONT_MATTER =
            Pattern.compile("^---[ \\t]*\\n(.*?)\\n---[ \\t]*\\n(.*)$", Pattern.DOTALL);

    private final Map<String, RagDocument> bySlug;
    private final int skippedFiles;

    @Autowired
    public DocLoader(MeterRegistry meterRegistry) {
        this(DEFAULT_LOCATION, meterRegistry);
    }

    DocLoader(String locationPattern, MeterRegistry meterRegistry) {
        List<Resource> resources = scan(locationPattern);

        List<RagDocument> parsed = new ArrayList<>(resources.size());
        int skipped = 0;

        for (Resource resource : resources) {
            try {
                parsed.add(parse(resource));
            } catch (RuntimeException e) {
                skipped++;
                log.error("Skipping help document {}: {}", resource.getFilename(), e.getMessage());
            }
        }

        parsed.sort(Comparator.comparing(RagDocument::slug));

        Map<String, RagDocument> loaded = new LinkedHashMap<>();
        for (RagDocument document : parsed) {
            if (loaded.putIfAbsent(document.slug(), document) != null) {
                skipped++;
                log.error("Skipping help document with duplicate slug '{}'", document.slug());
            }
        }

        this.bySlug = Collections.unmodifiableMap(loaded);
        this.skippedFiles = skipped;

        report(locationPattern);
        registerGauges(meterRegistry);
    }

    /**
     * Every document that parsed, ordered by slug so a reindex is reproducible.
     */
    public List<RagDocument> all() {
        return List.copyOf(bySlug.values());
    }

    public Optional<RagDocument> findBySlug(String slug) {
        return slug == null ? Optional.empty() : Optional.ofNullable(bySlug.get(slug));
    }

    /**
     * How many files were found but could not be used. Exposed for the gauge and for tests.
     */
    int skippedFiles() {
        return skippedFiles;
    }

    private List<Resource> scan(String locationPattern) {
        ResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(DocLoader.class.getClassLoader());
        try {
            return List.of(resolver.getResources(locationPattern));
        } catch (IOException e) {
            log.error("Failed to scan help documentation at {}: {}", locationPattern, e.getMessage());
            return List.of();
        }
    }

    private void report(String locationPattern) {
        if (bySlug.isEmpty()) {
            log.error("No help documentation loaded from {}. Every help question will be answered as out of scope.",
                    locationPattern);
            return;
        }

        log.info("Loaded {} help documents with {} question phrasings", bySlug.size(), phrasings());
        if (skippedFiles > 0) {
            log.warn("{} help document(s) were skipped. Their phrasings may still be in the index, "
                    + "and questions matching them will be answered as insufficient.", skippedFiles);
        }
    }

    private int phrasings() {
        return bySlug.values().stream().mapToInt(document -> document.questions().size()).sum();
    }

    private void registerGauges(MeterRegistry meterRegistry) {
        Gauge.builder("prostriver.help.documents.loaded", () -> bySlug.size())
                .description("Help documents parsed from the classpath at startup")
                .baseUnit("documents")
                .register(meterRegistry);

        Gauge.builder("prostriver.help.documents.skipped", () -> skippedFiles)
                .description("Help documents found at startup that could not be parsed and were skipped")
                .baseUnit("documents")
                .register(meterRegistry);

        Gauge.builder("prostriver.help.phrasings.loaded", this::phrasings)
                .description("Question phrasings across all loaded help documents")
                .baseUnit("phrasings")
                .register(meterRegistry);
    }

    private RagDocument parse(Resource resource) {
        String raw;
        try {
            raw = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("could not be read", e);
        }

        // Normalise line endings first, so the same file parses the same way whether it was checked
        // out with LF or CRLF.
        String normalised = raw.replace("\r\n", "\n").replace('\r', '\n');

        Matcher matcher = FRONT_MATTER.matcher(normalised);
        if (!matcher.matches()) {
            throw new IllegalStateException("no YAML front matter block delimited by ---");
        }

        Map<String, Object> meta = readFrontMatter(matcher.group(1));

        String body = matcher.group(2).strip();
        if (body.isEmpty()) {
            throw new IllegalStateException("the body is empty");
        }

        return new RagDocument(
                requiredString(meta, "slug"),
                requiredString(meta, "title"),
                requiredString(meta, "summary"),
                requiredQuestions(meta),
                body);
    }

    private Map<String, Object> readFrontMatter(String block) {
        // SafeConstructor keeps the parser to plain YAML types. These files are ours, but the parser
        // has no business instantiating arbitrary classes from a tag either way.
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

        Object parsed;
        try {
            parsed = yaml.load(block);
        } catch (RuntimeException e) {
            throw new IllegalStateException("front matter is not valid YAML: " + e.getMessage(), e);
        }

        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalStateException("front matter is not a YAML mapping");
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        map.forEach((key, value) -> meta.put(String.valueOf(key), value));
        return meta;
    }

    private String requiredString(Map<String, Object> meta, String key) {
        Object value = meta.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("front matter has no non-empty '" + key + "'");
        }
        return text.strip();
    }

    private List<String> requiredQuestions(Map<String, Object> meta) {
        Object value = meta.get("questions");
        if (!(value instanceof Collection<?> entries) || entries.isEmpty()) {
            throw new IllegalStateException("front matter has no non-empty 'questions' list");
        }

        List<String> questions = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (!(entry instanceof String text) || text.isBlank()) {
                throw new IllegalStateException("front matter has a blank 'questions' entry");
            }
            questions.add(text.strip());
        }
        return questions;
    }
}
