package com.kivojenko.plugin.manage_scheduled_tasks.model.node.tree;

import com.kivojenko.plugin.manage_scheduled_tasks.model.ScheduledMethod;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.MethodNode;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.NodeType;
import com.kivojenko.plugin.manage_scheduled_tasks.model.node.TypedNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleScheduledTreeNode extends DefaultMutableTreeNode {
    public SimpleScheduledTreeNode(List<ScheduledMethod> methods) {
        super("root");

        var groupedByPath = methods.stream()
                .collect(Collectors.groupingBy(
                        ScheduledMethod::className,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        groupedByPath.forEach((path, pathMethods) -> {
            var classNode = new TypedNode(path, NodeType.CLASS);
            add(classNode);
            pathMethods
                    .stream()
                    .map(m -> new MethodNode(m.methodName(), m.psiMethod(), m.cron()))
                    .forEach(classNode::add);
        });

    }

}
