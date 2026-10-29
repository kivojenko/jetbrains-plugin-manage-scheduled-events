package com.kivojenko.plugin.manage_scheduled_events.ui.tree;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;

@Getter
@RequiredArgsConstructor
public enum Grouping {
    CLASS("By class", AllIcons.Actions.GroupByClass),
    PACKAGE("By package", AllIcons.Actions.GroupByPackage),
    NONE("Jobs only", AllIcons.Actions.ListFiles);

    private final String label;
    private final Icon icon;

    public Grouping next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
