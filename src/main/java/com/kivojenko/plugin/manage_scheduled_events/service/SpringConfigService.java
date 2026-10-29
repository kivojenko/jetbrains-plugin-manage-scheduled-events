package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.psi.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reads and writes Spring Boot properties in {@code application.yml}, {@code application.yaml},
 * {@code application.properties} and their {@code application-<profile>.*} variants. Test resources are ignored.
 */
@Service(Service.Level.PROJECT)
public record SpringConfigService(Project project) {
    private static final Pattern CONFIG_FILE = Pattern.compile("application(?:-(.+))?\\.(yml|yaml|properties)");
    private static final List<String> EXTENSIONS = List.of("properties", "yml", "yaml");
    private static final List<String> PROFILE_KEYS = List.of("spring.config.activate.on-profile", "spring.profiles");

    /**
     * One value of a property.
     *
     * @param element the {@link IProperty} or {@link YAMLKeyValue} holding it
     * @param profile {@code null} for the default profile
     */
    public record Entry(PsiElement element, String value, @Nullable String profile) {
        public boolean isDefault() {
            return profile == null;
        }
    }

    public static SpringConfigService getInstance(Project project) {
        return project.getService(SpringConfigService.class);
    }

    /**
     * All values of {@code key} in the config files of {@code context}'s module, default profile first and
     * {@code .properties} before YAML, as Spring Boot gives {@code .properties} precedence. Keys are compared with
     * relaxed binding, so {@code jobs.reportCron} matches {@code jobs.report-cron}.
     */
    public List<Entry> find(String key, PsiElement context) {
        var target = canonical(key);
        List<Entry> entries = new ArrayList<>();

        for (var extension : EXTENSIONS) {
            for (var file : FilenameIndex.getAllFilesByExt(project, extension, scope(context))) {
                var matcher = CONFIG_FILE.matcher(file.getName());
                if (!matcher.matches() || ProjectFileIndex.getInstance(project).isInTestSourceContent(file)) continue;

                var fileProfile = matcher.group(1);
                var psiFile = PsiManager.getInstance(project).findFile(file);
                if (psiFile instanceof PropertiesFile properties) {
                    findInProperties(properties, target, fileProfile, entries);
                } else if (psiFile instanceof YAMLFile yaml) {
                    findInYaml(yaml, target, fileProfile, entries);
                }
            }
        }

        entries.sort(Comparator.comparing(Entry::isDefault).reversed());
        return entries;
    }

    /** Must run in a write action. */
    public boolean setValue(Entry entry, String value) {
        var element = entry.element();
        if (!element.isValid()) return false;

        if (element instanceof IProperty property) {
            property.setValue(value);
            return true;
        }
        if (element instanceof YAMLKeyValue keyValue) {
            // Quoted, as a cron may start with "*", which YAML reads as an alias
            var quote = keyValue.getValue() instanceof YAMLQuotedText text && text.isSingleQuote() ? "'" : "\"";
            var replacement = YAMLElementGenerator.getInstance(project)
                    .createYamlKeyValue("cron", quote + value + quote)
                    .getValue();
            if (replacement == null) return false;
            keyValue.setValue(replacement);
            return true;
        }
        return false;
    }

    private GlobalSearchScope scope(PsiElement context) {
        var module = ModuleUtilCore.findModuleForPsiElement(context);
        return module != null
                ? GlobalSearchScope.moduleWithDependenciesScope(module)
                : GlobalSearchScope.projectScope(project);
    }

    private static void findInProperties(PropertiesFile file, String target, @Nullable String profile,
                                         List<Entry> entries) {
        for (var property : file.getProperties()) {
            if (property.getKey() != null && canonical(property.getKey()).equals(target) && property.getValue() != null) {
                entries.add(new Entry(property.getPsiElement(), property.getValue(), profile));
            }
        }
    }

    /** A YAML file may hold several documents, each optionally limited to a profile. */
    private static void findInYaml(YAMLFile file, String target, @Nullable String fileProfile, List<Entry> entries) {
        for (var document : file.getDocuments()) {
            if (!(document.getTopLevelValue() instanceof YAMLMapping mapping)) continue;

            var profile = fileProfile;
            for (var profileKey : PROFILE_KEYS) {
                var keyValue = findInMapping(mapping, "", canonical(profileKey));
                if (keyValue != null) profile = keyValue.getValueText();
            }

            var keyValue = findInMapping(mapping, "", target);
            if (keyValue != null && keyValue.getValue() instanceof YAMLScalar) {
                entries.add(new Entry(keyValue, keyValue.getValueText(), profile));
            }
        }
    }

    /** Handles nested keys, dotted keys ({@code jobs.report: {cron: ...}}) and any mix of both. */
    private static @Nullable YAMLKeyValue findInMapping(YAMLMapping mapping, String prefix, String target) {
        for (var keyValue : mapping.getKeyValues()) {
            var path = prefix.isEmpty() ? canonical(keyValue.getKeyText()) : prefix + "." + canonical(keyValue.getKeyText());
            if (path.equals(target)) return keyValue;

            if (target.startsWith(path + ".") && keyValue.getValue() instanceof YAMLMapping child) {
                var found = findInMapping(child, path, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Spring's relaxed binding: case, dashes and underscores don't matter. */
    static String canonical(String key) {
        return key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
