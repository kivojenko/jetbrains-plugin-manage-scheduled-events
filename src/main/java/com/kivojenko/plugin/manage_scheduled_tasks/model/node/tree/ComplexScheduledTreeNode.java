package com.kivojenko.plugin.manage_scheduled_tasks.model.node.tree;

import com.kivojenko.plugin.manage_scheduled_tasks.model.ScheduledMethod;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.NodeType;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.TypedNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ComplexScheduledTreeNode extends TypedNode {
    public ComplexScheduledTreeNode(List<ScheduledMethod> methods) {
        super("root", NodeType.FOLDER);

        var groupedByPath = methods.stream()
                .collect(Collectors.groupingBy(
                        ScheduledMethod::filePath,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        groupedByPath.forEach((path, pathMethods) -> {
            var groupedByClass = pathMethods
                    .stream()
                    .collect(Collectors.groupingBy(
                            ScheduledMethod::className,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            TypedNode parent = findPath(path);

            groupedByClass
                    .forEach((className, classMethods) -> {
                        var classNode = new TypedNode(className, NodeType.CLASS);
                        parent.add(classNode);
                        classMethods
                                .stream()
                                .map(m -> new MethodNode(m.methodName(), m.psiMethod(), m.cron()))
                                .forEach(classNode::add);
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
