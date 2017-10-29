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

package util;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransApi {
    private static final String TRANS_API_HOST = "http://api.fanyi.baidu.com/api/trans/vip/translate";

    private String appid;
    private String securityKey;

    public TransApi(String appid, String securityKey) {
        this.appid = appid;
        this.securityKey = securityKey;
    }

    public String getTransResult(List<String> query, String from, String to) {
        Map<String, String> params = buildParams(query, from, to);

        return HttpGet.get(TRANS_API_HOST, params);
    }

    private Map<String, String> buildParams(List<String> querys, String from, String to) {
        Map<String, String> params = new HashMap<String, String>();
        StringBuilder lStringBuilder = new StringBuilder();
        for (String lS : querys) {
            lStringBuilder.append(lS).append("\n");
        }

        params.put("q", lStringBuilder.toString());
        params.put("from", from);
        params.put("to", to);
        params.put("appid", appid);

        // 随机数
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("salt", salt);

        // 签名
        String src = appid + lStringBuilder + salt + securityKey; // 加密前的原文
        params.put("sign", MD5.md5(src));
        return params;
    }

}
