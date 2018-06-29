/*
 * Copyright 2015-2018 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.ideamindmap.utils;

import com.igormaznitsa.ideamindmap.editor.MindMapDocumentEditor;
import com.igormaznitsa.ideamindmap.filetype.MindMapFileType;
import com.igormaznitsa.ideamindmap.lang.MMDFile;
import com.igormaznitsa.ideamindmap.lang.psi.PsiExtraFile;
import com.igormaznitsa.ideamindmap.swing.ColorChooserButton;
import com.igormaznitsa.ideamindmap.swing.FileEditPanel;
import com.igormaznitsa.ideamindmap.swing.PlainTextEditor;
import com.igormaznitsa.ideamindmap.swing.UriEditPanel;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.mindmap.model.MMapURI;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.model.logger.Logger;
import com.igormaznitsa.mindmap.model.logger.LoggerFactory;
import com.igormaznitsa.mindmap.swing.panel.DialogProvider;
import com.igormaznitsa.mindmap.swing.panel.HasPreferredFocusComponent;
import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.exception.MethodInvocationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public final class IdeaUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdeaUtils.class);
  private static final ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("/i18n/Bundle");

  public static final MMapURI EMPTY_URI;
  public static final String PROJECT_KNOWLEDGE_FOLDER_NAME = ".projectKnowledge";

  private static final boolean ALLOWS_TRANSACTION_GUARD = true;

  static {
    try {
      EMPTY_URI = new MMapURI("http://igormaznitsa.com/specialuri#empty"); //NOI18N
    } catch (URISyntaxException ex) {
      throw new Error("Unexpected exception", ex); //NOI18N
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static VirtualFile findMavenProjectRootForFile(@Nonnull final Project mainProject, @Nonnull final VirtualFile targetFile) {
    VirtualFile result = null;
    try {
      final Class<?> mavenProjectsManagerClass = Class.forName("org.jetbrains.idea.maven.project.MavenProjectsManager");
      final Object mavenProjectsManagerInstance = mavenProjectsManagerClass.getMethod("getInstance", Project.class).invoke(null, mainProject);

      final Method methodGetDirectoryFile = Class.forName("org.jetbrains.idea.maven.project.MavenProject").getMethod("getDirectoryFile");
      final List<?> listOfRootMaveProjects = (List<?>) mavenProjectsManagerClass.getMethod("getRootProjects").invoke(mavenProjectsManagerInstance);

      for (final Object mavenProject : listOfRootMaveProjects) {
        final VirtualFile directory = (VirtualFile) methodGetDirectoryFile.invoke(mavenProject);
        if (VfsUtil.isAncestor(directory, targetFile, false)) {
          result = directory;
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      LOGGER.info("can't find org...maven.project.MavenProjectsManager or org...maven.project.MavenProject in findMavenProjectRootForFile : "+ ex.getMessage());
    } catch (NoSuchMethodException ex) {
      LOGGER.error("NoSuchMethodException in findMavenProjectRootForFile", ex);
      throw new Error("NoSuchMethodException in findMavenProjectRootForFile", ex);
    } catch (Exception ex) {
      LOGGER.error("Error in findMavenProjectRootForFile", ex);
      throw new Error("Error in findMavenProjectRootForFile", ex);
    }
    return result;
  }

  @Nullable
  public static VirtualFile findPotentialRootFolderForModule(@Nullable final Module module) {
    VirtualFile moduleRoot = module == null ? null : module.getModuleFile();
    if (moduleRoot != null) {
      moduleRoot = moduleRoot.isDirectory() ? moduleRoot : moduleRoot.getParent();
      if (moduleRoot.getName().equals(".idea")) {
        moduleRoot = moduleRoot.getParent();
      }
    }
    return moduleRoot;
  }

  @Nullable
  public static Object callGetInstance(@Nonnull final String className) {
    Object result = null;
    try {
      final Class<?> foundClass = Class.forName(className);
      try {
        final Method instanceMethod = foundClass.getMethod("getInstance");
        if (Modifier.isStatic(instanceMethod.getModifiers())) {
          result = instanceMethod.invoke(null);
        }
      } catch (MethodInvocationException ex) {
        LOGGER.error("Error during dynamic getInstance() call", ex);
      } catch (Exception ex) {
        result = null;
      }
    } catch (final ClassNotFoundException ex) {
      result = null;
    }
    return result;
  }

  public static boolean submitTransactionLater(@Nonnull final Runnable runnable) {
    final Object transactionGuardInstance = callGetInstance("com.intellij.openapi.application.TransactionGuard");

    boolean result = false;
    if (transactionGuardInstance != null) {
      result = safeInvokeMethodNoResult(transactionGuardInstance, "submitTransactionLater", new Class<?>[]{Disposable.class, Runnable.class}, new Object[]{new Disposable() {
        @Override
        public void dispose() {
        }
      }, runnable});
    }

    return result;
  }

  public static void executeWriteAction(@Nullable final Project project, @Nullable final Document document, @Nonnull final Runnable action) {
    final Runnable wrapper = new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(action);
          }
        }, "MMD.executeWriteAction", null, document);
      }
    };

    if (ALLOWS_TRANSACTION_GUARD && submitTransactionLater(new Runnable() {
      @Override
      public void run() {
        wrapper.run();
      }
    })) {
      LOGGER.info("Using TransactionGuard for write action");
    } else {
      LOGGER.info("Using CommandProcessor for write action");
      wrapper.run();
    }
  }

  public static void executeReadAction(@Nullable final Project project, @Nullable final Document document, @Nonnull final Runnable action) {
    final Runnable wrapper = new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runReadAction(action);
          }
        }, "MMD>executeReadAction", null, document);
      }
    };

    if (ALLOWS_TRANSACTION_GUARD && submitTransactionLater(new Runnable() {
      @Override
      public void run() {
        wrapper.run();
      }
    })) {
      LOGGER.info("Using TransactionGuard for read action");
    } else {
      LOGGER.info("Using CommandProcessor for read action");
      wrapper.run();
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T safeInvokeMethodForResult(@Nonnull final Object instance, @Nonnull final T defaultResult, @Nonnull final String methodName, @Nonnull @MustNotContainNull final Class<?>[] argumentClasses, @Nonnull @MustNotContainNull final Object[] arguments) {
    final Method method;
    try {
      method = instance.getClass().getMethod(methodName, argumentClasses);
    } catch (NoSuchMethodException ex) {
      LOGGER.info("Can't find method '" + methodName + "' in class " + instance.getClass().getName());
      return defaultResult;
    }

    try {
      return (T) method.invoke(instance, arguments);
    } catch (Exception ex) {
      LOGGER.error("Error during call " + instance.getClass().getName() + "." + methodName + ", default result will be returned");
    }
    return defaultResult;
  }

  public static boolean safeInvokeMethodNoResult(@Nonnull final Object instance, @Nonnull final String methodName, @Nonnull @MustNotContainNull final Class<?>[] argumentClasses, @Nonnull @MustNotContainNull final Object[] arguments) {
    final Method method;
    try {
      method = instance.getClass().getMethod(methodName, argumentClasses);
    } catch (NoSuchMethodException ex) {
      LOGGER.info("Can't find method '" + methodName + "' in class " + instance.getClass().getName());
      return false;
    }

    try {
      method.invoke(instance, arguments);
      return true;
    } catch (Exception ex) {
      LOGGER.error("Error during call " + instance.getClass().getName() + "." + methodName);
    }
    return false;
  }

  @Nullable
  public static VirtualFile findKnowledgeFolderForModule(@Nullable final Module module, final boolean createIfMissing) {
    final VirtualFile rootFolder = IdeaUtils.findPotentialRootFolderForModule(module);
    final AtomicReference<VirtualFile> result = new AtomicReference<VirtualFile>();
    if (rootFolder != null) {
      result.set(rootFolder.findChild(PROJECT_KNOWLEDGE_FOLDER_NAME));
      if (result.get() == null || !result.get().isDirectory()) {
        if (createIfMissing) {
          CommandProcessor.getInstance().executeCommand(module.getProject(), new Runnable() {
            @Override
            public void run() {
              ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                  try {
                    result.set(VfsUtil.createDirectoryIfMissing(rootFolder, PROJECT_KNOWLEDGE_FOLDER_NAME));
                    LOGGER.info("Created knowledge folder for " + module);
                  } catch (IOException ex) {
                    LOGGER.error("Can't create knowledge folder for " + module, ex);
                  }
                }
              });
            }
          }, null, null);
        } else {
          result.set(null);
        }
      }
    }
    return result.get();
  }

  @Nullable
  public static Module findModuleForFile(@Nullable final Project project, @Nullable final VirtualFile file) {
    return project == null || file == null ? null : ModuleUtil.findModuleForFile(file, project);
  }

  public static boolean browseURI(final URI uri, final boolean useInsideBrowser) {
    try {
      BrowserUtil.browse(uri);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }

  public static void openInSystemViewer(@Nonnull final DialogProvider dialogProvider, @Nullable final VirtualFile theFile) {
    final File file = vfile2iofile(theFile);

    if (file == null) {
      LOGGER.error("Can't find file to open, null provided");
      dialogProvider.msgError(null, "Can't find file to open");
    } else {
      final Runnable startEdit = new Runnable() {
        @Override
        public void run() {
          boolean ok = false;
          if (Desktop.isDesktopSupported()) {
            final Desktop dsk = Desktop.getDesktop();
            if (dsk.isSupported(Desktop.Action.OPEN)) {
              try {
                dsk.open(file);
                ok = true;
              } catch (Throwable ex) {
                LOGGER.error("Can't open file in system viewer : " + file, ex);//NOI18N
              }
            }
          }
          if (!ok) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                dialogProvider.msgError(null, "Can't open file in system viewer! See the log!");//NOI18N
                Toolkit.getDefaultToolkit().beep();
              }
            });
          }
        }
      };
      final Thread thr = new Thread(startEdit, " MMDStartFileEdit");//NOI18N
      thr.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
          LOGGER.error("Detected uncaught exception in openInSystemViewer() for file " + file, e);
        }
      });

      thr.setDaemon(true);
      thr.start();
    }
  }

  public static boolean isDarkTheme() {
    return UIUtil.isUnderDarcula();
  }

  private static class DialogComponent extends DialogWrapper {
    private final JComponent component;
    private final JComponent prefferedComponent;

    public DialogComponent(final Project project, final String title, final JComponent component, final JComponent prefferedComponent, final boolean defaultButtonEnabled) {
      super(project, false, IdeModalityType.PROJECT);
      this.component = component;
      this.prefferedComponent = prefferedComponent == null ? component : prefferedComponent;
      init();
      setTitle(title);
      if (!defaultButtonEnabled)
        getRootPane().setDefaultButton(null);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return this.prefferedComponent == null ? this.component : this.prefferedComponent;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return this.component;
    }
  }

  public static File chooseFile(final Component parent, final boolean filesOnly, final String title, final File selectedFile, final FileFilter filter) {
    final JFileChooser chooser = new JFileChooser(selectedFile);

    chooser.setApproveButtonText("Select");
    if (filter != null)
      chooser.setFileFilter(filter);

    chooser.setDialogTitle(title);
    chooser.setMultiSelectionEnabled(false);

    if (filesOnly)
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    else
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

    if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile();
    } else {
      return null;
    }
  }

  public static String editText(final Project project, final String title, final String text) {
    final PlainTextEditor editor = new PlainTextEditor(project, text);
    editor.setPreferredSize(new Dimension(550, 450));

    final DialogComponent dialog = new DialogComponent(project, title, editor, editor.getEditor(), false);

    return dialog.showAndGet() ? editor.getEditor().getText() : null;
  }

  public static boolean plainMessageOkCancel(final Project project, final String title, final JComponent centerComponent) {
    final DialogComponent dialog = new DialogComponent(project, title, centerComponent,
            centerComponent instanceof HasPreferredFocusComponent ? ((HasPreferredFocusComponent) centerComponent).getComponentPreferredForFocus() : centerComponent, true);
    return dialog.showAndGet();
  }

  public static void plainMessageClose(final Project project, final String title, final JComponent centerComponent) {
    final DialogComponent dialog = new DialogComponent(project, title, centerComponent,
            centerComponent instanceof HasPreferredFocusComponent ? ((HasPreferredFocusComponent) centerComponent).getComponentPreferredForFocus() : centerComponent, true) {
      @Nonnull
      @Override
      protected Action[] createActions() {
        return new Action[]{new DialogWrapperAction(CommonBundle.getCloseButtonText()) {

          @Override
          protected void doAction(ActionEvent e) {
            doCancelAction();
          }
        }};
      }
    };
    dialog.show();
  }

  @Nullable
  public static File vfile2iofile(@Nullable final VirtualFile vfFile) {
    return vfFile == null ? null : VfsUtilCore.virtualToIoFile(vfFile);
  }

  public static Color extractCommonColorForColorChooserButton(final String colorAttribute, final Topic[] topics) {
    Color result = null;
    for (final Topic t : topics) {
      final Color color = html2color(t.getAttribute(colorAttribute), false);
      if (result == null) {
        result = color;
      } else {
        if (!result.equals(color)) {
          return ColorChooserButton.DIFF_COLORS;
        }
      }
    }
    return result;
  }

  public static Color html2color(final String str, final boolean hasAlpha) {
    Color result = null;
    if (str != null && !str.isEmpty() && str.charAt(0) == '#') {
      try {
        result = new Color(Integer.parseInt(str.substring(1), 16), hasAlpha);
      } catch (NumberFormatException ex) {
        LOGGER.warn(String.format("Can't convert %s to color", str));
      }
    }
    return result;
  }

  public static FileEditPanel.DataContainer editFilePath(final MindMapDocumentEditor editor, final String title, final File projectFolder, final FileEditPanel.DataContainer data) {
    final FileEditPanel filePathEditor = new FileEditPanel(editor.getDialogProvider(), projectFolder, data);

    filePathEditor.doLayout();
    filePathEditor.setPreferredSize(new Dimension(450, filePathEditor.getPreferredSize().height));

    if (plainMessageOkCancel(editor.getProject(), title, filePathEditor)) {
      final FileEditPanel.DataContainer result = filePathEditor.getData();
      if (result.isValid()) {
        return result;
      } else {
        Messages.showErrorDialog(editor.getMindMapPanel(), String.format(BUNDLE.getString("MMDGraphEditor.editFileLinkForTopic.errorCantFindFile"), result.getPathWithLine().getPath()), "Error");
        return null;
      }
    } else {
      return null;
    }
  }

  public static MMapURI editURI(final MindMapDocumentEditor editor, final String title, final MMapURI uri) {
    final UriEditPanel uriEditor = new UriEditPanel(uri == null ? null : uri.asString(false, false));

    uriEditor.doLayout();
    uriEditor.setPreferredSize(new Dimension(450, uriEditor.getPreferredSize().height));

    if (plainMessageOkCancel(editor.getProject(), title, uriEditor)) {
      final String text = uriEditor.getText();
      if (text.isEmpty()) {
        return EMPTY_URI;
      }
      try {
        if (!new URI(text).isAbsolute())
          throw new URISyntaxException(text, "URI is not absolute one");
        return new MMapURI(text.trim());
      } catch (URISyntaxException ex) {
        editor.getDialogProvider()
                .msgError(null, String.format(BUNDLE.getString("NbUtils.errMsgIllegalURI"), text));
        return null;
      }
    } else {
      return null;
    }
  }

  public static boolean isInProjectContentRoot(@Nonnull final Project project, @Nonnull final VirtualFile file) {
    for (final VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
      if (VfsUtil.isAncestor(root, file, false)) {
        return true;
      }
    }
    return false;
  }

  public static void showPopup(@Nonnull final String text, @Nonnull final MessageType type) {
    SwingUtils.safeSwing(new Runnable() {
      @Override
      public void run() {
        final JBPopupFactory factory = JBPopupFactory.getInstance();
        final BalloonBuilder builder = factory.createHtmlTextBalloonBuilder(StringEscapeUtils.escapeHtml(text), type, null);
        final Balloon balloon = builder.createBalloon();
        balloon.setAnimationEnabled(true);
        final Component frame = WindowManager.getInstance().findVisibleFrame();
        if (frame != null)
          balloon.show(new RelativePoint(frame, new Point(frame.getWidth(), frame.getHeight())), Balloon.Position.below);
      }
    });
  }

  @Nullable
  public static File findProjectFolder(@Nullable final Project project) {
    if (project == null) return null;
    return IdeaUtils.vfile2iofile(project.getBaseDir());
  }

  @Nullable
  public static File findProjectFolder(@Nullable final PsiElement element) {
    if (element == null)
      return null;
    return findProjectFolder(element.getProject());
  }

  public static List<PsiExtraFile> findPsiFileLinksForProjectScope(final Project project) {
    List<PsiExtraFile> result = new ArrayList<PsiExtraFile>();
    Collection<VirtualFile> virtualFiles = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, MindMapFileType.INSTANCE,
            GlobalSearchScope.allScope(project));
    for (VirtualFile virtualFile : virtualFiles) {
      final MMDFile simpleFile = (MMDFile) PsiManager.getInstance(project).findFile(virtualFile);
      if (simpleFile != null) {
        final PsiExtraFile[] fileLinks = PsiTreeUtil.getChildrenOfType(simpleFile, PsiExtraFile.class);
        if (fileLinks != null) {
          Collections.addAll(result, fileLinks);
        }
      }
    }
    return result;
  }

  @Nonnull
  public static GlobalSearchScope moduleScope(@Nonnull Project project, @Nullable Module module) {
    return module != null ? moduleScope(module) : GlobalSearchScope.projectScope(project);
  }

  @Nonnull
  public static GlobalSearchScope moduleScope(@Nonnull PsiElement element) {
    return moduleScope(element.getProject(), ModuleUtilCore.findModuleForPsiElement(element));
  }

  @Nonnull
  public static GlobalSearchScope moduleScope(@Nonnull Module module) {
    return GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module).uniteWith(module.getModuleContentWithDependenciesScope());
  }

  @Nonnull
  public static Pattern string2pattern(@Nonnull final String text, final int patternFlags){
    final StringBuilder result = new StringBuilder();

    for(final char c : text.toCharArray()){
      result.append("\\u"); //NOI18N
      final String code = Integer.toHexString(c).toUpperCase(Locale.ENGLISH);
      result.append("0000",0,4-code.length()).append(code); //NOI18N
    }

    return Pattern.compile(result.toString(), patternFlags);
  }
}
