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

package data.task;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.sargeraswang.util.ExcelUtil.ExcelLogs;
import com.sargeraswang.util.ExcelUtil.ExcelUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import action.ConvertToOtherLanguages;
import data.Log;
import data.SerializeUtil;
import data.StorageDataKey;
import language_engine.TranslationEngineType;
import language_engine.bing.BingTranslationApi;
import language_engine.google.GoogleTranslationApi;
import module.AndroidString;
import module.ExcelString;
import module.FilterRule;
import module.SupportedLanguages;

/**
 * Created by Wesley Lin on 12/1/14.
 */
public class GetTranslationTask extends Task.Backgroundable {

    private List<SupportedLanguages> selectedLanguages;
    private final List<AndroidString> sourceAndroidStrings;
    private double indicatorFractionFrame;
    private TranslationEngineType translationEngineType;
    private boolean override;
    private boolean inputChecked;
    private boolean outputChecked;
    private VirtualFile clickedFile;

    private static final String BingIdInvalid = "Invalid client id or client secret, " +
            "please check them <html><a href=\"https://datamarket.azure.com/developer/applications\">here</a></html>";
    private static final String BingQuotaExceeded = "Microsoft Translator quota exceeded, " +
            "please check your data usage <html><a href=\"https://datamarket.azure.com/account/datasets\">here</a></html>";

    private static final String GoogleErrorUnknown = "Error, please check API key in the settings panel.";
    private static final String NoTranslationString = "No strings to translate";
    private static final String GoogleDailyLimitError = "Daily Limit Exceeded, please note that Google Translation API " +
            "is a <html><a href=\"https://cloud.google.com/translate/v2/pricing\">paid service.</a></html>";

    private String errorMsg = null;

    /**
     * @param project
     * @param title                 任务名称，将显示在idea的下方任务栏中
     * @param selectedLanguages     需要翻译的语言
     * @param androidStrings        需要翻译的string
     * @param translationEngineType 翻译引擎，默认谷歌
     * @param override              是否覆盖
     * @param inputChecked          导入翻译
     * @param outputChecked         导出翻译
     * @param clickedFile           选中的文件
     */
    public GetTranslationTask(Project project,
                              String title,
                              List<SupportedLanguages> selectedLanguages,
                              List<AndroidString> androidStrings,
                              TranslationEngineType translationEngineType,
                              boolean override,
                              boolean inputChecked,
                              boolean outputChecked,
                              VirtualFile clickedFile) {
        super(project, title);
        this.selectedLanguages = selectedLanguages;
        this.sourceAndroidStrings = androidStrings;
        this.translationEngineType = translationEngineType;
        this.indicatorFractionFrame = 1.0d / (double) (this.selectedLanguages.size());
        this.override = override;
        this.inputChecked = inputChecked;
        this.outputChecked = outputChecked;
        this.clickedFile = clickedFile;
    }

    private static final String KEY = "Key";
    private static final String SOURCE = "Source Language";
    private static final String TARGET = "Target Language";

    @Override
    public void run(ProgressIndicator indicator) {

        if (outputChecked) {
            //导出已经存在的翻译文件
            Map<String, String> map = new LinkedHashMap<>();
            map.put("a", KEY);
            map.put("b", SOURCE);
            map.put("c", TARGET);

            //导出指定语言的当前xml文件，不进行翻译
            for (SupportedLanguages language : selectedLanguages) {
                //获取选中的语种

                //生成导出excel的路径
                String excelFileName = getResourcePathExcel(language);

                indicator.setText("save "+ language.getLanguageEnglishDisplayName() +"excle to" + excelFileName);
                //获取目标语言的文件路径
                String targetFileName = getResourcePath(language);
                //获取目标语言和源文件中的string内容，并生成map
                Collection<ExcelString> lExcelStringCollection = getTargetExcelStrings(sourceAndroidStrings, targetFileName);
                //导出到execl
                File f = new File(excelFileName);

                OutputStream out = null;
                try {
                    out = new FileOutputStream(f);
                    ExcelUtil.exportExcel(map, lExcelStringCollection, out);
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else if (inputChecked) {
            //从指定的xml文件中，导入选中的语言
            for (int i = 0; i < selectedLanguages.size(); i++) {

                //需要翻译的目标语言
                SupportedLanguages language = selectedLanguages.get(i);
                String excelFileName = getResourcePathExcel(language);
                File f = new File(excelFileName);
                InputStream inputStream = null;
                List<AndroidString> translationAndroidStrings = new ArrayList<>();
                try {
                    inputStream = new FileInputStream(f);
                    ExcelLogs logs = new ExcelLogs();
                    Collection<Map> importExcel = ExcelUtil.importExcel(Map.class, inputStream, "yyyy/MM/dd HH:mm:ss", logs, 0);

                    for (Map m : importExcel) {
                        AndroidString a = new AndroidString();
                        a.setKey(String.valueOf(m.get(KEY)));
                        a.setValue(String.valueOf(m.get(TARGET)));
                        translationAndroidStrings.add(a);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                //把翻译的结果写进文件名为fileName的xml中
                String targetFileName = getResourcePath(language);
                List<AndroidString> fileContent = getTargetAndroidStrings(sourceAndroidStrings, translationAndroidStrings, targetFileName, override);
                writeAndroidStringToLocal(myProject, targetFileName, fileContent);
            }

        } else {
            for (int i = 0; i < selectedLanguages.size(); i++) {

                //需要翻译的目标语言
                SupportedLanguages language = selectedLanguages.get(i);
                List<List<AndroidString>> filterAndSplitString
                        = splitAndroidString(filterAndroidString(sourceAndroidStrings, language, override), translationEngineType);
                List<AndroidString> translationAndroidStrings = new ArrayList<AndroidString>();
                for (int j = 0; j < filterAndSplitString.size(); j++) {
                    List<AndroidString> lList = getTranslationEngineResult(
                            filterAndSplitString.get(j),
                            language,
//                        SupportedLanguages.English,
                            SupportedLanguages.Chinese_Simplified,
                            translationEngineType
                    );

                    if (lList == null) {
                        break;
                    } else {
                        translationAndroidStrings.addAll(lList);
                    }

                    //idea 的任务栏显示正在进行的任务
                    indicator.setFraction(indicatorFractionFrame * (double) (i)
                            + indicatorFractionFrame / filterAndSplitString.size() * (double) (j));
                    indicator.setText("Translating to " + language.getLanguageEnglishDisplayName()
                            + " (" + language.getLanguageDisplayName() + ")");
                    Log.i("Translating to : " + language.getLanguageEnglishDisplayName());

                }
                //把翻译的结果写进文件名为fileName的xml中
                String targetFileName = getResourcePath(language);
                List<AndroidString> fileContent = getTargetAndroidStrings(sourceAndroidStrings, translationAndroidStrings, targetFileName, override);
                writeAndroidStringToLocal(myProject, targetFileName, fileContent);
            }
        }
    }


    @Override
    public void onSuccess() {
        if (errorMsg == null || errorMsg.isEmpty())
            return;
        ConvertToOtherLanguages.showErrorDialog(getProject(), errorMsg);
    }

    /**
     * 生成对应语言的文件名称，例如values-en/string.xml
     *
     * @param language
     * @return
     */
    private String getResourcePath(SupportedLanguages language) {
        String resPath = clickedFile.getPath().substring(0,
                clickedFile.getPath().indexOf("/res/") + "/res/".length());

        return resPath + "values-" + language.getAndroidStringFolderNameSuffix()
                + "/" + clickedFile.getName();
    }

    /**
     * 导出翻译文件的路径，例如values-en.xls
     *
     * @param language
     * @return
     */
    private String getResourcePathExcel(SupportedLanguages language) {
        String resPath = clickedFile.getPath().substring(0,
                clickedFile.getPath().indexOf("/res/") + "/res/".length());

        return resPath + "values-" + language.getAndroidStringFolderNameSuffix() + ".xls";
    }


    /**
     * 使用选择的翻译引擎,进行翻译
     *
     * @param needToTranslatedString
     * @param targetLanguageCode
     * @param sourceLanguageCode
     * @param translationEngineType
     * @return
     */
    // todo: if got error message, should break the background task
    private List<AndroidString> getTranslationEngineResult(@NotNull List<AndroidString> needToTranslatedString,
                                                           @NotNull SupportedLanguages targetLanguageCode,
                                                           @NotNull SupportedLanguages sourceLanguageCode,
                                                           TranslationEngineType translationEngineType) {

        List<String> querys = AndroidString.getAndroidStringValues(needToTranslatedString);

        if (querys.size() == 0) {
            errorMsg = NoTranslationString;
            return null;
        }

        List<String> result = null;

        switch (translationEngineType) {
            case Bing:
                String accessToken = BingTranslationApi.getAccessToken();
                if (accessToken == null) {
                    errorMsg = BingIdInvalid;
                    return null;
                }
                result = BingTranslationApi.getTranslatedStringArrays2(accessToken, querys, sourceLanguageCode, targetLanguageCode);

                if ((result == null || result.isEmpty()) && !querys.isEmpty()) {
                    errorMsg = BingQuotaExceeded;
                    return null;
                }
                break;
            case Google:
                result = GoogleTranslationApi.getTranslationJSON(querys, targetLanguageCode, sourceLanguageCode);
                if (result == null) {
                    errorMsg = GoogleErrorUnknown;
                    return null;
                } else if (result.isEmpty() && !querys.isEmpty()) {
                    errorMsg = GoogleDailyLimitError;
                    return null;
                }
                Log.i("query Result list: " + result.toString());

                break;
        }

        List<AndroidString> translatedAndroidStrings = new ArrayList<AndroidString>();

        for (int i = 0; i < needToTranslatedString.size(); i++) {
            translatedAndroidStrings.add(new AndroidString(
                    needToTranslatedString.get(i).getKey(), result.get(i)));
        }
        return translatedAndroidStrings;
    }

    /**
     * 把需要翻译的字符串列表，切分为50个一组，避免一次请求过多
     *
     * @param origin
     * @param engineType 翻译引擎，不同的引擎，一组的大小可能不一样
     * @return
     */
    private List<List<AndroidString>> splitAndroidString(List<AndroidString> origin, TranslationEngineType engineType) {

        List<List<AndroidString>> splited = new ArrayList<List<AndroidString>>();
        int splitFragment = 50;
        switch (engineType) {
            case Bing:
                splitFragment = 50;
                break;
            case Google:
                splitFragment = 50;
                break;
        }

        if (origin.size() <= splitFragment) {
            splited.add(origin);
        } else {
            int count = (origin.size() % splitFragment == 0) ? (origin.size() / splitFragment) : (origin.size() / splitFragment + 1);
            for (int i = 1; i <= count; i++) {
                int end = i * splitFragment;
                if (end > origin.size()) {
                    end = origin.size();
                }

                splited.add(origin.subList((i - 1) * splitFragment, end));
            }
        }
        return splited;
    }

    /**
     * 过滤翻译的xml中的string
     * 这个规律规则是在settings中设置的，被过滤的string不翻译
     *
     * @param origin
     * @param language
     * @param override
     * @return
     */
    private List<AndroidString> filterAndroidString(List<AndroidString> origin,
                                                    SupportedLanguages language,
                                                    boolean override) {
        List<AndroidString> result = new ArrayList<AndroidString>();

        VirtualFile targetStringFile = LocalFileSystem.getInstance().findFileByPath(
                getResourcePath(language));
        List<AndroidString> targetAndroidStrings = new ArrayList<AndroidString>();
        if (targetStringFile != null) {
            try {
                targetAndroidStrings = AndroidString.getAndroidStringsList(targetStringFile.contentsToByteArray());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        String rulesString = PropertiesComponent.getInstance().getValue(StorageDataKey.SettingFilterRules);
        List<FilterRule> filterRules = new ArrayList<FilterRule>();
        if (rulesString == null) {
            filterRules.add(FilterRule.DefaultFilterRule);
        } else {
            filterRules = SerializeUtil.deserializeFilterRuleList(rulesString);
        }
//        Log.i("targetAndroidString: " + targetAndroidStrings.toString());
        for (AndroidString androidString : origin) {
            // filter rules
            if (FilterRule.inFilterRule(androidString.getKey(), filterRules))
                continue;

            // override
            if (!override && !targetAndroidStrings.isEmpty()) {
                // 已经存在的androidString，不需要重写
                // check if there is the androidString in this file
                // if there is, filter it
                if (isAndroidStringListContainsKey(targetAndroidStrings, androidString.getKey())) {
                    continue;
                }
            }

            result.add(androidString);
        }

        return result;
    }

    /**
     * 翻译后的结果，根据是否需要覆盖已经存在的string，返回最终结果。
     * 读取已经存在的目标文件，根据是否需要重写，来生成新的string map，最终写入新的文件
     *
     * @param sourceAndroidStrings     源xml文件中的string
     * @param translatedAndroidStrings 翻译后的string
     * @param fileName                 目标xml文件
     * @param override                 是否覆盖
     * @return
     */
    private static List<AndroidString> getTargetAndroidStrings(List<AndroidString> sourceAndroidStrings,
                                                               List<AndroidString> translatedAndroidStrings,
                                                               String fileName,
                                                               boolean override) {

        if (translatedAndroidStrings == null) {
            translatedAndroidStrings = new ArrayList<AndroidString>();
        }

        VirtualFile existenceFile = LocalFileSystem.getInstance().findFileByPath(fileName);
        List<AndroidString> existenceAndroidStrings = null;
        if (existenceFile != null && !override) {
            //目标文件存在，且不需要覆盖，那么就需要读取已经存在的string，在保存的时候过滤这些文件
            try {
                existenceAndroidStrings = AndroidString.getAndroidStringsList(existenceFile.contentsToByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            existenceAndroidStrings = new ArrayList<AndroidString>();
        }

//        Log.i("sourceAndroidStrings: " + sourceAndroidStrings,
//                "translatedAndroidStrings: " + translatedAndroidStrings,
//                "existenceAndroidStrings: " + existenceAndroidStrings);

        List<AndroidString> targetAndroidStrings = new ArrayList<AndroidString>();

        for (int i = 0; i < sourceAndroidStrings.size(); i++) {
            //这里一定要先获得list里面的数据，然后在创建一个AndroidString
            AndroidString lAndroidString = sourceAndroidStrings.get(i);
            AndroidString resultString = new AndroidString(lAndroidString);

            // if override is checked, skip setting the existence value, for performance issue
            if (!override) {
                //不重写
                String existenceValue = getAndroidStringValueInList(existenceAndroidStrings, resultString.getKey());
                if (existenceValue != null) {
                    resultString.setValue(existenceValue);
                }
            }

            String translatedValue = getAndroidStringValueInList(translatedAndroidStrings, resultString.getKey());
            if (translatedValue != null) {
                resultString.setValue(translatedValue);
            }

            targetAndroidStrings.add(resultString);
        }
//        Log.i("targetAndroidStrings: " + targetAndroidStrings);
        return targetAndroidStrings;
    }


    /**
     * 在源文件和翻译文件中找到相同key对应的值
     * 以源文件为主，显示所有源文件的内容，如果对应的key，在翻译文件中没有，相应的翻译置空
     *
     * @param sourceAndroidStrings 源xml文件中的string
     * @param targetFileName       目标xml文件
     * @return
     */
    private static Collection<ExcelString> getTargetExcelStrings(List<AndroidString> sourceAndroidStrings,
                                                                 String targetFileName) {


        VirtualFile existenceFile = LocalFileSystem.getInstance().findFileByPath(targetFileName);
        List<AndroidString> existenceAndroidStrings = null;
        Collection<ExcelString> excelStringList = new ArrayList<ExcelString>();


        if (existenceFile != null) {
            //目标文件存在，且不需要覆盖，那么就需要读取已经存在的string，在保存的时候过滤这些文件
            try {
                existenceAndroidStrings = AndroidString.getAndroidStringsList(existenceFile.contentsToByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.i("sourceAndroidStrings: " + sourceAndroidStrings,
                "existenceAndroidStrings: " + existenceAndroidStrings);

        if (existenceAndroidStrings == null || existenceAndroidStrings.size() == 0) {
            return null;
        }
        for (AndroidString lAndroidString : sourceAndroidStrings) {

            //xml中string 键值对的键
            String key = lAndroidString.getKey();
            //xml中string 键值对的值，这个值源文件中的
            String sourceValue = lAndroidString.getValue();


            //xml中string 键值对的值，这个值是翻译文件中的
            String translatedValue = "";

            for (AndroidString lAndroidString1 : existenceAndroidStrings) {
                if (key.equals(lAndroidString1.getKey())) {
                    translatedValue = lAndroidString1.getValue();
                    break;
                }
            }

            ExcelString lExcelString = new ExcelString(key, sourceValue, translatedValue);
            excelStringList.add(lExcelString);
        }

        return excelStringList;
    }


    /**
     * 把翻译的结果写进，指定的xml文件中
     *
     * @param myProject
     * @param filePath    xml文件的路径  例如：values-en/string.xml
     * @param fileContent xml所以string的内容
     */
    private static void writeAndroidStringToLocal(final Project myProject, String filePath, List<AndroidString> fileContent) {
        Log.i("write AndroidString To Local", filePath);
        File file = new File(filePath);
        final VirtualFile virtualFile;
        boolean fileExits = true;
        try {
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                fileExits = false;
                file.createNewFile();
            }
            //Change by GodLikeThomas FIX: Appeared Messy code under windows --start; 
            //FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            //BufferedWriter writer = new BufferedWriter(fileWriter);
            //writer.write(getFileContent(fileContent));
            //writer.close();
            FileOutputStream fos = new FileOutputStream(file.getAbsoluteFile());
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
//            Log.i("写入翻译结果到文件",filePath);

            osw.write(getFileContent(fileContent));
            osw.close();
            //Change by GodLikeThomas FIX: Appeared Messy code under windows --end;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fileExits) {
            virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
            if (virtualFile == null)
                return;
            virtualFile.refresh(true, false, new Runnable() {
                @Override
                public void run() {
                    openFileInEditor(myProject, virtualFile);
                }
            });
        } else {
            virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
            openFileInEditor(myProject, virtualFile);
        }
    }

    /**
     * 在idea中打开指定的文件
     *
     * @param myProject
     * @param file      需要打开的文件
     */
    private static void openFileInEditor(final Project myProject, @Nullable final VirtualFile file) {
        if (file == null)
            return;

        // run in UI thread:
        //    https://theantlrguy.atlassian.net/wiki/display/~admin/Intellij+plugin+development+notes#Intellijplugindevelopmentnotes-GUIandthreads,backgroundtasks
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final FileEditorManager editorManager = FileEditorManager.getInstance(myProject);
                editorManager.openFile(file, true);
            }
        });
    }

    private static String getFileContent(List<AndroidString> fileContent) {
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
        String stringResourceHeader = "<resources>\n\n";
        String stringResourceTail = "</resources>\n";

        StringBuilder sb = new StringBuilder();
        sb.append(xmlHeader).append(stringResourceHeader);
        for (AndroidString androidString : fileContent) {
            sb.append("\t").append(androidString.toString()).append("\n");
        }
        sb.append("\n").append(stringResourceTail);
        return sb.toString();
    }

    private static boolean isAndroidStringListContainsKey(List<AndroidString> androidStrings, String key) {
        List<String> keys = AndroidString.getAndroidStringKeys(androidStrings);
        return keys.contains(key);
    }

    public static String getAndroidStringValueInList(List<AndroidString> androidStrings, String key) {
        for (AndroidString androidString : androidStrings) {
            if (androidString.getKey().equals(key)) {
                return androidString.getValue();
            }
        }
        return null;
    }

}
