package com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.tree;

import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.NodeType;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.TypedNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ComplexScheduledTreeNode extends TypedNode {
    public ComplexScheduledTreeNode(List<MethodNode> methods) {
        super("root", NodeType.FOLDER);

        methods
                .stream()
                .collect(Collectors.groupingBy(MethodNode::getFilePath, LinkedHashMap::new, Collectors.toList()))
                .forEach((path, pathMethods) -> {
                    TypedNode parent = findPath(path);
                    pathMethods
                            .stream()
                            .collect(Collectors.groupingBy(
                                    MethodNode::getClassName,
                                    LinkedHashMap::new,
                                    Collectors.toList()))
                            .forEach((className, classMethods) -> {
                                var classNode = new TypedNode(className, NodeType.CLASS);
                                parent.add(classNode);
                                classMethods.forEach(classNode::add);
                            });

                });
    }

    private TypedNode findPath(String path) {
        TypedNode parent = this;

        for (String part : path.split("\\.")) {
            if (!part.isBlank()) parent = findChild(parent, part);
        }

        return parent;
    }

    private static TypedNode findChild(TypedNode parent, String name) {
        TypedNode child;

        for (int i = 0; i < parent.getChildCount(); i++) {
            child = (TypedNode) parent.getChildAt(i);
            if (name.equals(child.getUserObject().toString())) return child;
        }

        child = new TypedNode(name, NodeType.PACKAGE);
        parent.add(child);

        return child;
    }
}
