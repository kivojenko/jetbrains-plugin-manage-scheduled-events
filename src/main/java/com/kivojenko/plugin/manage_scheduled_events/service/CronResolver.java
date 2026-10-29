package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves the cron behind a source element and writes changes back to wherever it comes from:
 * <ul>
 *     <li>a string literal or constant</li>
 *     <li>a {@code ${key:default}} placeholder (only where Spring resolves them, e.g. {@code @Scheduled(cron)})</li>
 *     <li>a field or parameter annotated {@code @Value("${key}")}</li>
 *     <li>a getter chain on a {@code @ConfigurationProperties} class, e.g. {@code jobs.getReport().getCron()}</li>
 *     <li>any of the above passed in as an argument, e.g. {@code schedule(String cron)} called once as
 *     {@code schedule("0 0 3 * * *")}</li>
 * </ul>
 * A cron that is only known at runtime, e.g. read from a database, is reported with the expression it comes from.
 * Property values are looked up in {@code application.yml} / {@code .properties} via {@link SpringConfigService}.
 */
public final class CronResolver {
    public static final String NO_CRON = "<no cron>";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");
    private static final String VALUE = "org.springframework.beans.factory.annotation.Value";
    private static final String CONFIGURATION_PROPERTIES =
            "org.springframework.boot.context.properties.ConfigurationProperties";
    private static final int MAX_RUNTIME_SOURCE_LENGTH = 60;

    /**
     * @param literal the string holding the placeholder, so its default can be edited; {@code null} if it isn't a
     *                plain literal
     */
    private record PropertyReference(String key, @Nullable String defaultValue, @Nullable PsiLiteralExpression literal) {
    }

    private CronResolver() {
    }

    /**
     * Must run in a read action.
     *
     * @param placeholders whether {@code ${...}} in a string is a placeholder; Spring resolves them in
     *                     {@code @Scheduled}, Quartz does not
     */
    public static CronValue resolve(@Nullable PsiElement element, boolean placeholders) {
        if (!(element instanceof PsiExpression source)) return CronValue.of(NO_CRON);
        var expression = traceArgument(source);

        var reference = findPropertyReference(expression, placeholders);
        if (reference == null) {
            var constant = constantString(expression);
            if (constant != null) return CronValue.of(constant);
            if (expression instanceof PsiLiteralExpression) return CronValue.of(NO_CRON);
            return CronValue.runtime(NO_CRON, StringUtil.shortenTextWithEllipsis(
                    StringUtil.collapseWhiteSpace(expression.getText()), MAX_RUNTIME_SOURCE_LENGTH, 0));
        }

        var entries = SpringConfigService.getInstance(expression.getProject()).find(reference.key(), expression);
        var base = entries.stream().filter(SpringConfigService.Entry::isDefault).findFirst();
        var cron = base.map(SpringConfigService.Entry::value).orElse(reference.defaultValue());

        var overriding = entries.stream()
                .filter(entry -> !entry.isDefault() && !Objects.equals(entry.value(), cron))
                .map(SpringConfigService.Entry::profile)
                .distinct()
                .toList();

        if (cron == null) {
            return new CronValue("${" + reference.key() + "}", reference.key(), overriding,
                    "Not set in application.yml / .properties", null);
        }
        return new CronValue(cron, reference.key(), overriding, null, null);
    }

    /**
     * Where a new cron is written: the default-profile property value, else the placeholder's default, else the
     * string literal. Found in a read action, as it needs indexes, then written in a write action.
     *
     * @param entry          the config value to change
     * @param literal        the string literal to rewrite, when there is no config value
     * @param placeholderKey set when {@code literal} is a {@code ${key:default}} placeholder whose default is rewritten
     */
    public record Target(@Nullable SpringConfigService.Entry entry, @Nullable PsiLiteralExpression literal,
                         @Nullable String placeholderKey) {

        /** Must run in a write action. @return {@code false} if the source changed since it was found. */
        public boolean write(String newCron) {
            if (entry != null) {
                return SpringConfigService.getInstance(entry.element().getProject()).setValue(entry, newCron);
            }
            if (literal == null || !literal.isValid()) return false;
            return replaceLiteral(literal, placeholderKey == null ? newCron : "${" + placeholderKey + ":" + newCron + "}");
        }
    }

    /**
     * Must run in a read action.
     *
     * @return {@code null} if there is nowhere to write a cron, e.g. a shared constant or a property without a value
     */
    public static @Nullable Target findTarget(@Nullable PsiElement element, boolean placeholders) {
        if (!(element instanceof PsiExpression source)) return null;
        var expression = traceArgument(source);

        var reference = findPropertyReference(expression, placeholders);
        if (reference == null) {
            // A constant may be used elsewhere, so only literals are rewritten
            return expression instanceof PsiLiteralExpression literal ? new Target(null, literal, null) : null;
        }

        var base = SpringConfigService.getInstance(expression.getProject()).find(reference.key(), expression).stream()
                .filter(SpringConfigService.Entry::isDefault)
                .findFirst();
        if (base.isPresent()) return new Target(base.get(), null, null);

        if (reference.literal() != null && reference.defaultValue() != null) {
            return new Target(null, reference.literal(), reference.key());
        }
        return null;
    }

    /** {@link #findTarget} and {@link Target#write} at once. Must run in a write action. */
    public static boolean write(@Nullable PsiElement element, boolean placeholders, String newCron) {
        var target = findTarget(element, placeholders);
        return target != null && target.write(newCron);
    }

    /**
     * Follows a method parameter back to the argument passed for it, as long as the method is called from exactly one
     * place, e.g. {@code cronSchedule(cron)} in {@code createSchedule(String cron)} to {@code job.getScheduleCron()} in
     * {@code createSchedule(job.getScheduleCron())}. Stops at {@code @Value} parameters, which hold the cron themselves.
     */
    private static PsiExpression traceArgument(PsiExpression expression) {
        var current = expression;
        Set<PsiParameter> visited = new HashSet<>();
        while (PsiUtil.skipParenthesizedExprDown(current) instanceof PsiReferenceExpression reference
                && reference.resolve() instanceof PsiParameter parameter
                && visited.add(parameter)
                && !parameter.hasAnnotation(VALUE)
                && parameter.getDeclarationScope() instanceof PsiMethod method) {
            var argument = singleArgument(method, method.getParameterList().getParameterIndex(parameter));
            if (argument == null) break;
            current = argument;
        }
        return current;
    }

    /** The argument at {@code index} of the only call to {@code method}; {@code null} if it is called elsewhere too. */
    private static @Nullable PsiExpression singleArgument(PsiMethod method, int index) {
        if (index < 0 || (method.isVarArgs() && index == method.getParameterList().getParametersCount() - 1)) {
            return null;
        }

        PsiCall onlyCall = null;
        for (var reference : MethodReferencesSearch.search(method, method.getUseScope(), true)) {
            if (onlyCall != null || !(reference.getElement().getParent() instanceof PsiCall call)) return null;
            onlyCall = call;
        }
        if (onlyCall == null || onlyCall.getArgumentList() == null) return null;

        var arguments = onlyCall.getArgumentList().getExpressions();
        return index < arguments.length ? arguments[index] : null;
    }

    private static @Nullable PropertyReference findPropertyReference(PsiExpression expression, boolean placeholders) {
        if (placeholders) {
            var reference = parsePlaceholder(expression);
            if (reference != null) return reference;
        }

        if (expression instanceof PsiReferenceExpression ref
                && ref.resolve() instanceof PsiModifierListOwner owner
                && owner.getModifierList() != null) {
            var value = owner.getModifierList().findAnnotation(VALUE);
            if (value != null && value.findAttributeValue("value") instanceof PsiExpression valueExpression) {
                return parsePlaceholder(valueExpression);
            }
        }

        if (expression instanceof PsiMethodCallExpression call) {
            var key = configurationPropertiesKey(call);
            if (key != null) return new PropertyReference(key, null, null);
        }
        return null;
    }

    private static @Nullable PropertyReference parsePlaceholder(PsiExpression expression) {
        var text = constantString(expression);
        if (text == null) return null;

        var matcher = PLACEHOLDER.matcher(text.strip());
        if (!matcher.matches()) return null;

        var literal = expression instanceof PsiLiteralExpression l ? l : null;
        return new PropertyReference(matcher.group(1).strip(), matcher.group(2), literal);
    }

    /** {@code jobs.getReport().getCron()} on {@code @ConfigurationProperties("jobs")} is {@code jobs.report.cron}. */
    private static @Nullable String configurationPropertiesKey(PsiMethodCallExpression call) {
        Deque<String> names = new ArrayDeque<>();
        PsiMethod rootMethod = null;
        PsiExpression current = call;

        while (current instanceof PsiMethodCallExpression getter) {
            var method = getter.resolveMethod();
            if (method == null || !method.getParameterList().isEmpty()) return null;
            names.addFirst(propertyName(method.getName()));
            rootMethod = method;
            current = getter.getMethodExpression().getQualifierExpression();
        }
        if (current == null || rootMethod == null) return null;

        var rootClass = PsiUtil.resolveClassInType(current.getType());
        if (rootClass == null) rootClass = rootMethod.getContainingClass();
        var annotation = rootClass == null ? null : rootClass.getAnnotation(CONFIGURATION_PROPERTIES);
        if (annotation == null) return null;

        var prefix = annotationString(annotation, "prefix");
        if (prefix == null || prefix.isBlank()) prefix = annotationString(annotation, "value");
        if (prefix == null || prefix.isBlank()) return null;

        return prefix + "." + String.join(".", names);
    }

    /** {@code getReportCron} and record accessor {@code reportCron} are both {@code report-cron}. */
    private static String propertyName(String methodName) {
        var name = methodName;
        if (name.startsWith("get") && name.length() > 3) name = name.substring(3);
        else if (name.startsWith("is") && name.length() > 2) name = name.substring(2);
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }

    private static @Nullable String annotationString(PsiAnnotation annotation, String attribute) {
        return annotation.findDeclaredAttributeValue(attribute) instanceof PsiExpression expression
                ? constantString(expression)
                : null;
    }

    private static @Nullable String constantString(PsiExpression expression) {
        var value = JavaPsiFacade.getInstance(expression.getProject())
                .getConstantEvaluationHelper()
                .computeConstantExpression(expression);
        return value instanceof String string ? string : null;
    }

    private static boolean replaceLiteral(PsiLiteralExpression literal, String text) {
        var factory = JavaPsiFacade.getElementFactory(literal.getProject());
        literal.replace(factory.createExpressionFromText("\"" + text + "\"", literal));
        return true;
    }
}
