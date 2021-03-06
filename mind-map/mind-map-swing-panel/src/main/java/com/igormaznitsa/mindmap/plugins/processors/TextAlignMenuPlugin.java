/*
 * Copyright 2019 Igor Maznitsa.
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

package com.igormaznitsa.mindmap.plugins.processors;

import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.common.utils.ArrayUtils;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.plugins.PopUpSection;
import com.igormaznitsa.mindmap.plugins.api.AbstractPopupMenuItem;
import com.igormaznitsa.mindmap.plugins.api.PluginContext;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanel;
import com.igormaznitsa.mindmap.swing.panel.ui.TextAlign;
import com.igormaznitsa.mindmap.swing.services.IconID;
import com.igormaznitsa.mindmap.swing.services.ImageIconServiceProvider;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

public class TextAlignMenuPlugin extends AbstractPopupMenuItem {

  private static final ResourceBundle BUNDLE = java.util.ResourceBundle.getBundle("com/igormaznitsa/mindmap/swing/panel/Bundle");
  private static final Icon ICON = ImageIconServiceProvider.findInstance().getIconForId(IconID.ICON_TEXT_ALIGN);
  private static final Icon ICON_CENTER = ImageIconServiceProvider.findInstance().getIconForId(IconID.ICON_TEXT_ALIGN_CENTER);
  private static final Icon ICON_LEFT = ImageIconServiceProvider.findInstance().getIconForId(IconID.ICON_TEXT_ALIGN_LEFT);
  private static final Icon ICON_RIGHT = ImageIconServiceProvider.findInstance().getIconForId(IconID.ICON_TEXT_ALIGN_RIGHT);

  @Override
  @Nullable
  public JMenuItem makeMenuItem(@Nonnull final PluginContext context, @Nullable final Topic activeTopic) {
    final JMenu result = UI_COMPO_FACTORY.makeMenu(BUNDLE.getString("TextAlign.Plugin.MenuTitle"));
    result.setIcon(ICON);

    final ButtonGroup buttonGroup = UI_COMPO_FACTORY.makeButtonGroup();

    final Topic[] workTopics;
    if (activeTopic == null) {
      workTopics = context.getSelectedTopics();
    } else {
      workTopics = ArrayUtils.append(activeTopic, context.getSelectedTopics());
    }

    final TextAlign sharedTextAlign = findSharedTextAlign(workTopics);

    final JRadioButtonMenuItem menuLeft = UI_COMPO_FACTORY.makeRadioButtonMenuItem(BUNDLE.getString("TextAlign.Plugin.MenuTitle.Left"), ICON_LEFT, TextAlign.LEFT == sharedTextAlign);
    final JRadioButtonMenuItem menuCenter = UI_COMPO_FACTORY.makeRadioButtonMenuItem(BUNDLE.getString("TextAlign.Plugin.MenuTitle.Center"), ICON_CENTER, TextAlign.CENTER == sharedTextAlign);
    final JRadioButtonMenuItem menuRight = UI_COMPO_FACTORY.makeRadioButtonMenuItem(BUNDLE.getString("TextAlign.Plugin.MenuTitle.Right"), ICON_RIGHT, TextAlign.RIGHT == sharedTextAlign);

    buttonGroup.add(menuLeft);
    buttonGroup.add(menuCenter);
    buttonGroup.add(menuRight);

    result.add(menuLeft);
    result.add(menuCenter);
    result.add(menuRight);

    menuLeft.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull final ActionEvent e) {
        setAlignValue(context.getPanel(), workTopics, TextAlign.LEFT);
      }
    });

    menuCenter.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull final ActionEvent e) {
        setAlignValue(context.getPanel(), workTopics, TextAlign.CENTER);
      }
    });

    menuRight.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@Nonnull final ActionEvent e) {
        setAlignValue(context.getPanel(), workTopics, TextAlign.RIGHT);
      }
    });

    return result;
  }

  private void setAlignValue(@Nonnull final MindMapPanel panel, @Nonnull @MustNotContainNull final Topic[] topics, @Nonnull final TextAlign align) {
    for (final Topic t : topics) {
      t.setAttribute("align", align.name().toLowerCase(Locale.ENGLISH));
    }
    panel.doNotifyModelChanged(true);
  }

  @Nullable
  private TextAlign findSharedTextAlign(@Nonnull @MustNotContainNull final Topic[] topics) {
    TextAlign result = null;

    for (final Topic t : topics) {
      final TextAlign topicAlign = TextAlign.findForName(t.getAttribute("align"));
      if (result == null) {
        result = topicAlign;
      } else if (result != topicAlign) {
        return null;
      }
    }

    return result == null ? TextAlign.CENTER : result;
  }

  @Override
  @Nonnull
  public PopUpSection getSection() {
    return PopUpSection.MAIN;
  }

  @Override
  public boolean needsTopicUnderMouse() {
    return true;
  }

  @Override
  public boolean needsSelectedTopics() {
    return true;
  }

  @Override
  public int getOrder() {
    return 10;
  }

}
