package com.kivojenko.plugin.manage_scheduled_events.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.PsiJavaParserFacadeImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.kivojenko.plugin.manage_scheduled_events.ui.tree.node.MethodNode;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.util.List;

@Service(Service.Level.PROJECT)
public final class ShedLockService {
    private final Project project;

    private final PsiJavaParserFacadeImpl psiParserFacade;
    private final JavaCodeStyleManager styleManager;
    private PsiClass lockClass;

    public ShedLockService(Project project) {
        this.project = project;

        psiParserFacade = new PsiJavaParserFacadeImpl(project);
        styleManager = JavaCodeStyleManager.getInstance(project);

        DumbService.getInstance(project).runWhenSmart(() ->
                ApplicationManager.getApplication()
                        .executeOnPooledThread(() -> {
                            var psiFacade = JavaPsiFacade.getInstance(project);
                            var name = SchedulerLock.class.getCanonicalName();
                            lockClass = ReadAction.compute(() -> psiFacade.findClass(name,
                                    GlobalSearchScope.allScope(project)));
                        })
        );
    }

    public void lockAll(List<MethodNode> methods) {
        WriteCommandAction.runWriteCommandAction(project, () -> methods.forEach(this::addShedLockAnnotation));
    }

    private void addShedLockAnnotation(MethodNode method) {
        if (method.getShedLockAnnotation() != null || lockClass == null) return;

        var context = method.getAnnotation().getContext();
        if (context == null || !(context.getContainingFile() instanceof PsiJavaFile file)) return;

        if (file.findImportReferenceTo(lockClass) == null) {
            styleManager.addImport(file, lockClass);
        }

        String lock = "@SchedulerLock(name = \"" + method.getClassName() + "_" + method.getName() + "Lock\")";
        PsiAnnotation element = psiParserFacade.createAnnotationFromText(lock, context);

        context.addAfter(element, method.getAnnotation());
    }


    public void unlockAll(List<MethodNode> methods) {
        WriteCommandAction.runWriteCommandAction(project, () -> methods.forEach(this::removeShedLockAnnotation));
    }

    public void removeShedLockAnnotation(MethodNode method) {
        if (method.getShedLockAnnotation() != null) {
            method.getShedLockAnnotation().delete();
            removeImports(method);
        }
    }

    public void removeImports(MethodNode method) {
        if (method.getAnnotation().getContainingFile() instanceof PsiJavaFile file) {
            styleManager.removeRedundantImports(file);
        }
    }
}
