/*
 * Copyright 2014-2015 Wesley Lin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package action;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import data.Log;
import data.StorageDataKey;
import data.task.GetTranslationTask;
import language_engine.TranslationEngineType;
import module.AndroidString;
import module.SupportedLanguages;
import ui.MultiSelectDialog;

/**
 * Created by Wesley Lin on 11/26/14.
 */
public class ConvertToOtherLanguages extends AnAction implements MultiSelectDialog.OnOKClickedListener {

    private static final String LOCALIZATION_TITLE = "Choose alternative string resources";
    private static final String LOCALIZATION_MSG = "Warning: " +
            "The string resources are translated by %s, " +
            "try keeping your string resources simple, so that the result is more satisfied.";
    private static final String OVERRIDE_EXITS_STRINGS = "Override the existing strings";

    private Project project;
    private List<AndroidString> androidStringsInSelectFile = null;

    public TranslationEngineType defaultTranslationEngine = TranslationEngineType.Google;

    private VirtualFile selectedFile;

    public ConvertToOtherLanguages() {
        super("Convert to other languages", null, IconLoader.getIcon("/icons/globe.png"));
    }

    /**
     * 控制菜单选项是否可见
     * @param e
     */
    @Override
    public void update(AnActionEvent e) {
        final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE) ;

        boolean isStringXML = isStringXML(file);
        e.getPresentation().setEnabled(isStringXML);
        e.getPresentation().setVisible(isStringXML);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {

        project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        //获取点击的文件
        selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        Log.i("clicked file: " + selectedFile.getPath());

        //PropertiesComponent 键值对，是否有保存的翻译引擎
        if (PropertiesComponent.getInstance().isValueSet(StorageDataKey.SettingLanguageEngine)) {
            defaultTranslationEngine = TranslationEngineType.fromName(
                    PropertiesComponent.getInstance().getValue(StorageDataKey.SettingLanguageEngine));
        }

        try {
            androidStringsInSelectFile = AndroidString.getAndroidStringsList(selectedFile.contentsToByteArray());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

//        for (int i = 0; i < androidStringsInStringFile.size(); i++) {
//            Log.i("[" + (i + 1) + "]: " + androidStringsInStringFile.get(i).toString());
//        }

        if (androidStringsInSelectFile == null || androidStringsInSelectFile.isEmpty()) {
            showErrorDialog(project, "Target file does not contain any strings.");
            return;
        }

        // show dialog
        MultiSelectDialog multiSelectDialog = new MultiSelectDialog(project,
                String.format(LOCALIZATION_MSG, defaultTranslationEngine.getDisplayName()),
                LOCALIZATION_TITLE,
                OVERRIDE_EXITS_STRINGS,
                PropertiesComponent.getInstance(project).getBoolean(StorageDataKey.OverrideCheckBoxStatus, false),
                defaultTranslationEngine,
                false);
        multiSelectDialog.setOnOKClickedListener(this);
        multiSelectDialog.show();
    }

    @Override
    public void onClick(List<SupportedLanguages> selectedLanguages, boolean overrideChecked) {
        // set consistence data
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        propertiesComponent.setValue(StorageDataKey.OverrideCheckBoxStatus, String.valueOf(overrideChecked));

        List<SupportedLanguages> allData = SupportedLanguages.getAllSupportedLanguages(defaultTranslationEngine);

        for (SupportedLanguages language : allData) {
            //选中的翻译引擎，是否包含要翻译的目标语言，保存起来
            propertiesComponent.setValue(StorageDataKey.SupportedLanguageCheckStatusPrefix + language.getLanguageCode(),
                    String.valueOf(selectedLanguages.contains(language)));
        }

        new GetTranslationTask(project, "Translation in progress, using " + defaultTranslationEngine.getDisplayName(),
                selectedLanguages, androidStringsInSelectFile, defaultTranslationEngine, overrideChecked, selectedFile)
                .setCancelText("Translation has been canceled").queue();
    }

    public static void showErrorDialog(Project project, String msg) {
        Messages.showErrorDialog(project, msg, "Error");
    }

    /**
     *
     * @param file
     * @return
     */
    private static boolean isStringXML(@Nullable VirtualFile file) {
        if (file == null)
            return false;

        if (!file.getName().equals("strings.xml"))
            //不包括strings.xml ，返回false
            return false;

        if (file.getParent() == null)
            //没有上一级文件夹 ，返回返回false
            return false;

        // only show popup menu for English strings
        if (!file.getParent().getName().equals("values") && !file.getParent().getName().startsWith("values-en"))
            return false;

        return true;
    }
}
