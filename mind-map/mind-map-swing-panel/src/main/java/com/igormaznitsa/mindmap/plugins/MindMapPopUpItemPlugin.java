/*
 * Copyright 2016 Igor Maznitsa.
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
package com.igormaznitsa.mindmap.plugins;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JMenuItem;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.annotation.Weight;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.swing.panel.DialogProvider;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanel;

public interface MindMapPopUpItemPlugin extends MindMapPlugin {
  @Weight(Weight.Unit.LIGHT)
  @Nullable
  JMenuItem getPluginMenuItem(
      @Nonnull MindMapPanel panel, 
      @Nonnull DialogProvider dialogProvider, 
      @Nonnull PopUpSection section, 
      @Nullable Topic topic, 
      @Nullable @MustNotContainNull Topic[] selectedTopics, 
      @Nullable MindMapPopUpItemCustomProcessor customProcessor);
}