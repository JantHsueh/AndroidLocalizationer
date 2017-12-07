/*
 * Copyright 2014-2017 Wesley Lin
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

package language_engine.baidu;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import module.SupportedLanguages;
import baiduUtil.HttpGet;
import baiduUtil.TransApi;

public class BaiduTranslationApi {


    private static final String APP_ID = "20171029000091508";
    private static final String SECURITY_KEY = "4qcbuAjnkzVFj1u0D7Il";

    /**
     * @param querys
     * @param targetLanguageCode
     * @param sourceLanguageCode
     * @return
     */
    public static List<String> getTranslationJSON(@NotNull List<String> querys,
                                                  @NotNull SupportedLanguages targetLanguageCode,
                                                  @NotNull SupportedLanguages sourceLanguageCode) {
        if (querys.isEmpty())
            return null;

        TransApi api = new TransApi(APP_ID, SECURITY_KEY);

        String query = "";
        for (int i = 0; i < querys.size(); i++) {
            query += ("q=" + URLEncoder.encode(querys.get(i)));
            if (i != querys.size() - 1) {
                query += "&";
            }
        }

        String from = sourceLanguageCode.getLanguageCode();
        String to = targetLanguageCode.getLanguageCode();

        String getResult = api.getTransResult(querys, from, to);
//        Log.i("do get result: " + getResult);

        JsonObject jsonObject = new JsonParser().parse(getResult).getAsJsonObject();
        if (jsonObject.get("error") != null) {
            JsonObject error = jsonObject.get("error").getAsJsonObject().get("errors").getAsJsonArray().get(0).getAsJsonObject();
            if (error == null)
                return null;

            if (error.get("reason").getAsString().equals("dailyLimitExceeded"))
                return new ArrayList<String>();
            return null;
        } else {
            JsonArray translations = jsonObject.get("trans_result").getAsJsonArray();
            if (translations != null) {
                List<String> result = new ArrayList<String>();
                for (int i = 0; i < translations.size(); i++) {
                    result.add(HttpGet.decode(translations.get(i).getAsJsonObject().get("dst").getAsString()));
                }
                return result;
            }
        }
        return null;
    }
}
