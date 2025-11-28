package com.kivojenko.plugin.manage_scheduled_events.ui.panel.subpanel.button;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public abstract class ScheduledButton<A extends AnAction> extends ActionButton {
    public ScheduledButton(@NotNull A action) {
        super(action, action.getTemplatePresentation(), ActionPlaces.UNKNOWN, getDimension());
    }

    public static Dimension getDimension() {
        return new Dimension(30, 28);
    }
}
