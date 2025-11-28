package com.kivojenko.plugin.manage_scheduled_events.ui.tree.model.node;

import lombok.Getter;

import javax.swing.tree.DefaultMutableTreeNode;

@Getter
public class TypedNode extends DefaultMutableTreeNode {
    private final NodeType type;

    public TypedNode(Object userObject, NodeType type) {
        super(userObject);
        this.type = type;
    }
}
