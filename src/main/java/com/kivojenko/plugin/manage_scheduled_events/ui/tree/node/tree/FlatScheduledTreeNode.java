package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.tree;

import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.JobNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class FlatScheduledTreeNode extends DefaultMutableTreeNode {
    public FlatScheduledTreeNode(List<JobNode> methods) {
        super("root");
        methods.forEach(this::add);
    }
}
