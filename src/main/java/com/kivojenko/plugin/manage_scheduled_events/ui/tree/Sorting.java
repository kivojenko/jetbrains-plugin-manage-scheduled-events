package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.icons.AllIcons;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Sorting {
    NAME("By name", AllIcons.ObjectBrowser.Sorted),
    FREQUENCY("By frequency", AllIcons.Vcs.History);

    private static final Comparator<String> TEXT = String.CASE_INSENSITIVE_ORDER;

    private final String label;
    private final Icon icon;

    public Sorting next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Most frequent first; invalid or never-running crons go last. Ties fall back to name order. */
    public List<JobNode> sort(List<JobNode> methods, Grouping grouping) {
        var byName = nameComparator(grouping);
        if (this == NAME) return methods.stream().sorted(byName).toList();

        Map<JobNode, Duration> intervals = methods.stream().collect(Collectors.toMap(
                Function.identity(),
                m -> m.getAverageInterval().orElse(Duration.ofSeconds(Long.MAX_VALUE))));
        return methods.stream().sorted(Comparator.<JobNode, Duration>comparing(intervals::get).thenComparing(byName)).toList();
    }

    private static Comparator<JobNode> nameComparator(Grouping grouping) {
        Comparator<JobNode> byJob = Comparator.comparing(JobNode::getName, TEXT);
        Comparator<JobNode> byClass = Comparator.comparing(JobNode::getClassName, TEXT);
        return switch (grouping) {
            case CLASS -> byClass.thenComparing(byJob);
            case PACKAGE -> Comparator.comparing(JobNode::getFilePath, TEXT).thenComparing(byClass).thenComparing(byJob);
            case NONE -> byJob.thenComparing(byClass);
        };
    }
}
