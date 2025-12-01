package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.tree;

import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.NodeType;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleScheduledTreeNode extends DefaultMutableTreeNode {
    public SimpleScheduledTreeNode(List<MethodNode> methods) {
        super("root");

        methods
                .stream()
                .collect(Collectors.groupingBy(MethodNode::getClassName, LinkedHashMap::new, Collectors.toList()))
                .forEach((path, pathMethods) -> {
                    var classNode = new TypedNode(path, NodeType.CLASS);
                    add(classNode);
                    pathMethods.forEach(classNode::add);
                });

    }

}
