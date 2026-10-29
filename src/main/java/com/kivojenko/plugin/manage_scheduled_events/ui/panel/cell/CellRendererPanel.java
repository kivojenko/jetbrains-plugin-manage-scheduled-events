package com.kivojenko.plugin.manage_scheduled_events.ui.panel.cell;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;

/**
 * A tree row: name on the left, cron and ShedLock icon on the right, the description wrapping centered in between, or
 * on its own lines under the name when the row is too narrow.
 */
public class CellRendererPanel extends JBPanel<CellRendererPanel> {
    private static final int GAP = 8;
    private static final int MIN_DESCRIPTION_WIDTH = 80;

    private final JBLabel nameLabel = new JBLabel();
    private final JTextPane descriptionArea = new JTextPane();
    private final JBLabel cronLabel = new JBLabel();
    private final JBLabel iconLabel = new JBLabel();
    private final JBPanel<?> rightPanel = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(GAP), 0));

    public CellRendererPanel() {
        super(new BorderLayout(JBUI.scale(GAP), 0));

        nameLabel.setVerticalAlignment(SwingConstants.TOP);

        var centered = new SimpleAttributeSet();
        StyleConstants.setAlignment(centered, StyleConstants.ALIGN_CENTER);
        descriptionArea.getStyledDocument().setParagraphAttributes(0, 0, centered, false);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setBorder(null);
        descriptionArea.setFont(nameLabel.getFont());
        descriptionArea.setForeground(JBColor.GRAY);

        rightPanel.add(cronLabel);
        rightPanel.add(iconLabel);
        rightPanel.setOpaque(false);

        add(nameLabel, BorderLayout.WEST);
        add(descriptionArea, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setNode(TypedNode node) {
        nameLabel.setIcon(
                switch (node.getType()) {
                    case FOLDER -> AllIcons.Nodes.Folder;
                    case PACKAGE -> AllIcons.Nodes.Package;
                    case CLASS -> AllIcons.Nodes.Class;
                    case METHOD -> AllIcons.Nodes.Method;
                }
        );

        if (node instanceof JobNode jobNode) {
            nameLabel.setText(jobNode.getName());
            if (jobNode instanceof MethodNode methodNode && methodNode.isShedLockEnabled()) {
                Icon icon;
                Color color;

                if (methodNode.getShedLockAnnotation() == null) {
                    icon = AllIcons.Ide.Readwrite;
                    color = JBColor.RED;
                } else {
                    icon = AllIcons.Ide.Readonly;
                    color = JBColor.GRAY;
                }
                iconLabel.setIcon(IconUtil.colorize(icon, color));
            } else {
                iconLabel.setIcon(null);
            }

            descriptionArea.setText(describe(jobNode));
            descriptionArea.setForeground(jobNode.isBroken() ? JBColor.RED : JBColor.GRAY);

            cronLabel.setText(String.format("%16s", jobNode.getCron()));

        } else {
            nameLabel.setText(node.getUserObject().toString());
            descriptionArea.setText("");
            cronLabel.setText("");
        }
    }

    /** The cron description, followed by the property it is read from, if any. */
    public static String describe(JobNode node) {
        var origin = node.getCronOrigin();
        return origin == null ? node.getCronDescription() : node.getCronDescription() + " · " + origin;
    }

    /** Whether name, description and cron all fit side by side in {@code width}. A width of 0 or less always fits. */
    public boolean fitsOnOneLine(int width) {
        var text = descriptionArea.getText();
        if (width <= 0 || text.isEmpty()) return true;

        int descriptionWidth = descriptionArea.getFontMetrics(descriptionArea.getFont()).stringWidth(text);
        return nameLabel.getPreferredSize().width + descriptionWidth + rightPanel.getPreferredSize().width
                + 2 * JBUI.scale(GAP) <= width;
    }

    /**
     * Sizes the row to {@code width}, wrapping the description onto as many lines as it needs. With
     * {@code belowName}, the description gets its own lines under the name, indented to line up with the name's text.
     * A width of 0 or less keeps the natural single-line size.
     */
    public void fitTo(int width, boolean belowName) {
        if (width <= 0) return;

        if (belowName && !descriptionArea.getText().isEmpty()) {
            var icon = nameLabel.getIcon();
            descriptionArea.setBorder(JBUI.Borders.emptyLeft(icon == null ? 0 : icon.getIconWidth() + nameLabel.getIconTextGap()));
            var left = new SimpleAttributeSet();
            StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);
            descriptionArea.getStyledDocument().setParagraphAttributes(0, descriptionArea.getDocument().getLength(), left, false);

            remove(descriptionArea);
            add(descriptionArea, BorderLayout.SOUTH);

            descriptionArea.setSize(width, Short.MAX_VALUE);
            int height = Math.max(nameLabel.getPreferredSize().height, rightPanel.getPreferredSize().height)
                    + descriptionArea.getPreferredSize().height;
            setPreferredSize(new Dimension(width, height));
            return;
        }

        int descriptionWidth = Math.max(JBUI.scale(MIN_DESCRIPTION_WIDTH),
                width - nameLabel.getPreferredSize().width - rightPanel.getPreferredSize().width - 2 * JBUI.scale(GAP));
        // A text pane wraps, and reports the height its text needs at the width it was given
        descriptionArea.setSize(descriptionWidth, Short.MAX_VALUE);

        int height = Math.max(nameLabel.getPreferredSize().height,
                Math.max(descriptionArea.getPreferredSize().height, rightPanel.getPreferredSize().height));
        setPreferredSize(new Dimension(width, height));
    }

    public void setText(String text) {
        nameLabel.setText(text);
        nameLabel.setIcon(null);

        descriptionArea.setText("");
        cronLabel.setText("");
    }
}
