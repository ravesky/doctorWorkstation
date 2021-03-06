package model;

import Utils.LoggerUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.istack.internal.NotNull;
import model.bean.StringResponseBean;
import model.bean.UniversalResponseBean;
import okhttp3.*;

import javax.swing.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Model<E> {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    public static final MediaType MEDIA_TYPE_FORM = MediaType.parse("multipart/form-data");
    public static final MediaType MEDIA_STREAM = MediaType.parse("application/octet-stream");
    private final String nullExceptionTag = "服务器返回为空";
    private Integer limit = 200;
    private Integer skip = 0;
    private int connectTimeout = 20;
    private int readOrWriteTimeout = 30;

    private OkHttpClient client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
            .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();

    private RequestBody requestBody;
    private Request request;
    private Request.Builder requestBuilder;
    private MultipartBody.Builder multipartBuilder;
    private Call call;
    private String responseString = null;
    private UniversalResponseBean<E> universalResponseBean = new UniversalResponseBean<E>();
    private StringResponseBean stringResponseBean = new StringResponseBean();

    private Type type;
    private static Gson gson = new GsonBuilder()
            .serializeNulls()
            .create();

    public static Gson getGson() {
        return new GsonBuilder()
                .serializeNulls()
                .create();
    }

    public void convert(UniversalResponseBean bean) {
        if (bean.getData() != null && !bean.getData().equals("")) {
            String s = getGson().toJson(bean.getData());
            bean.setData(getGson().fromJson(s, type));
        }
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }

    /**
     * @param apiUrl_Post      post的url
     * @param jsonData         要发的json数据
     * @param responseListener 返回 List<T> 数据
     */
    public void postData(String apiUrl_Post, Object jsonData, final @NotNull OnResponseListener<E> responseListener) {
        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();
        requestBuilder = new Request.Builder().url(apiUrl_Post);

        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
        requestBody = RequestBody.create(JSON, gson.toJson(jsonData));
        request = requestBuilder.post(requestBody).build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    responseString = response.body().string();
                    log(responseString);

                    if (checkResponseIsNull(responseString)) {
                        finishOnMainThread(responseListener, null, new Exception(nullExceptionTag));
                        return;
                    }
                    Type type = new TypeToken<UniversalResponseBean<E>>() {
                    }.getType();
                    universalResponseBean = gson.fromJson(responseString, type);
                    if (Model.this.type != null) {
                        convert(universalResponseBean);
                    }
                    finishOnMainThread(responseListener, universalResponseBean, null);

                } catch (Exception e) {
                    finishOnMainThread(responseListener, universalResponseBean, e);
                    e.printStackTrace();
                }
            }
        });
    }

    public void postData(String apiUrl_Post, Object jsonData, final @NotNull OnStringResponseListener responseListener) {
        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();
        requestBuilder = new Request.Builder().url(apiUrl_Post);
        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
        requestBody = RequestBody.create(JSON, gson.toJson(jsonData));
        request = requestBuilder.post(requestBody).build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    responseString = response.body().string();
                    log(responseString);

                    if (checkResponseIsNull(responseString)) {
                        finishOnMainThread(responseListener, null, new Exception(nullExceptionTag));
                        return;
                    }
//                    stringResponseBean = gson.fromJson(responseString, StringResponseBean.class);

                    finishOnMainThread(responseListener, responseString, null);

                } catch (Exception e) {
                    finishOnMainThread(responseListener, responseString, e);
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * @param apiUrl_Get       get的url
     * @param params           要传的值
     * @param responseListener 返回 List<T> 数据
     */
    public void getData(String apiUrl_Get, LinkedHashMap<String, String> params, final @NotNull OnResponseListener<E> responseListener) {
        String finalUrl = apiUrl_Get;
        if (params == null) {
            log("getdata params is null");

        } else {
            finalUrl = attachHttpGetParams(apiUrl_Get, params);
        }
        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();

        requestBuilder = new Request.Builder().url(finalUrl);

        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
//        requestBuilder.addHeader("limit", limit.toString());
//        requestBuilder.addHeader("skip", skip.toString());
        requestBuilder.get();

        request = requestBuilder.build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    responseString = response.body().string();
                    log(responseString);
                    if (checkResponseIsNull(responseString)) {
                        finishOnMainThread(responseListener, null, new Exception(nullExceptionTag));
                        return;
                    }

                    Type type = new TypeToken<UniversalResponseBean<E>>() {
                    }.getType();
                    universalResponseBean = gson.fromJson(responseString, type);
                    if (Model.this.type != null) {
                        convert(universalResponseBean);
                    }
                    finishOnMainThread(responseListener, universalResponseBean, null);

                    return;
                } catch (Exception e) {
                    finishOnMainThread(responseListener, null, e);

                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * @param apiUrl_Get       get的url
     * @param data           要传的值
     * @param responseListener 返回 List<T> 数据
     */
    public void getData(String apiUrl_Get, LinkedHashMap<String, String> data, final @NotNull OnStringResponseListener responseListener) {
        String finalUrl = apiUrl_Get;
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (!apiUrl_Get.equals(ApiUrl.Get.LOGIN_USER_URL)) {
            if (data == null) {//查询全部
                params.put("limit",limit.toString());
                params.put("skip",skip.toString());
                finalUrl = attachHttpGetParams(apiUrl_Get, params);
            }else {//条件查询
                try {
                    finalUrl = apiUrl_Get+"?where="+URLEncoder.encode(gson.toJson(data),"UTF-8").replace("%3A",":");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

        } else {//登录
            params=data;
            finalUrl = attachHttpGetParams(apiUrl_Get, params);
        }


        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();
        log(finalUrl);
        requestBuilder = new Request.Builder().url(finalUrl);

        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
//        requestBuilder.addHeader("limit", limit.toString());
//        requestBuilder.addHeader("skip", skip.toString());
        requestBuilder.get();

        request = requestBuilder.build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    responseString = response.body().string();
                    log(responseString);
                    if (checkResponseIsNull(responseString)) {
                        finishOnMainThread(responseListener, null, new Exception(nullExceptionTag));
                        return;
                    }

                    Type type = new TypeToken<UniversalResponseBean<E>>() {
                    }.getType();
//                    universalResponseBean = gson.fromJson(responseString, type);
                    if (Model.this.type != null) {
                        convert(universalResponseBean);
                    }
                    finishOnMainThread(responseListener, responseString, null);

                    return;
                } catch (Exception e) {
                    finishOnMainThread(responseListener, null, e);

                    e.printStackTrace();
                }

            }
        });
    }



    /**
     * @param apiUrl_Put       put的url
     * @param jsonData         要修改的json数据/string
     * @param responseListener 返回 List<T> 数据
     */
    public void putData(String apiUrl_Put, Object jsonData, final @NotNull OnResponseListener<E> responseListener) {
        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();

        requestBuilder = new Request.Builder().url(apiUrl_Put);

        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
        requestBody = RequestBody.create(JSON, gson.toJson(jsonData));
        request = requestBuilder.put(requestBody).build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {
                    responseString = response.body().string();
                    log(responseString);

                    if (checkResponseIsNull(responseString)) {
                        finishOnMainThread(responseListener, null, new Exception(nullExceptionTag));
                        return;
                    }

                    Type type = new TypeToken<UniversalResponseBean<E>>() {
                    }.getType();
                    universalResponseBean = gson.fromJson(responseString, type);
                    if (Model.this.type != null) {
                        convert(universalResponseBean);
                    }
                    finishOnMainThread(responseListener, universalResponseBean, null);

                } catch (Exception e) {
                    finishOnMainThread(responseListener, null, e);
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * @param apiUrl_Put       put的url
     * @param jsonData         要修改的json数据/string
     * @param responseListener 返回 List<T> 数据
     */
    public void putData(String apiUrl_Put, Object jsonData, final @NotNull OnStringResponseListener responseListener) {
        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();

        requestBuilder = new Request.Builder().url(apiUrl_Put);

        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
        requestBody = RequestBody.create(JSON, gson.toJson(jsonData));
        request = requestBuilder.put(requestBody).build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {
                    responseString = response.body().string();
                    log(responseString);

                    if (checkResponseIsNull(responseString)) {
                        finishOnMainThread(responseListener, null, new Exception(nullExceptionTag));
                        return;
                    }

                    finishOnMainThread(responseListener, responseString, null);

                } catch (Exception e) {
                    finishOnMainThread(responseListener, null, e);
                    e.printStackTrace();
                }

            }
        });
    }

    /**
     * @param apiUrl_Del
     * @param id               要删除的id
     * @param responseListener
     */
    public void delData(String apiUrl_Del, String id, OnStringResponseListener responseListener) {
        client = new OkHttpClient.Builder().connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readOrWriteTimeout, TimeUnit.SECONDS)
                .writeTimeout(readOrWriteTimeout, TimeUnit.SECONDS).build();
        requestBuilder = new Request.Builder().url(apiUrl_Del + "/" + id);

        requestBuilder.addHeader("X-Bmob-Application-Id", Akey.appId);
        requestBuilder.addHeader("X-Bmob-REST-API-Key", Akey.restId);
        request = requestBuilder.delete().build();
        if (call != null) {
            call.cancel();
        }
        call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishOnMainThread(responseListener, null, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    responseString = response.body().string();
                    log(responseString);

                    finishOnMainThread(responseListener, responseString, null);

                    return;
                } catch (Exception e) {
                    finishOnMainThread(responseListener, null, e);
                    e.printStackTrace();
                }
            }
        });

    }


    /**
     * 为HttpGet 的 url 方便的添加多个name value 参数。
     *
     * @param url
     * @param params
     * @return
     */
    public static String attachHttpGetParams(String url, LinkedHashMap<String, String> params) {

        Iterator<String> keys = params.keySet().iterator();
        Iterator<String> values = params.values().iterator();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("?");

        for (int i = 0; i < params.size(); i++) {
            String value = null;
            try {
                value = URLEncoder.encode(values.next(), "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }

            stringBuffer.append(keys.next() + "=" + value);
            if (i != params.size() - 1) {
                stringBuffer.append("&");
            }
        }
        return url + stringBuffer.toString();
    }


    public Boolean checkResponseIsNull(String responseString) {
        if (responseString != null) {
            if (responseString.length() > 2) {
                return false;
            }
        }
        return true;
    }


    public void finishOnMainThread(OnResponseListener<E> onResponseListener, UniversalResponseBean<E> bean, Exception e) {
        SwingUtilities.invokeLater(() -> {
            onResponseListener.onFinish(bean.getData(), e);
        });
    }

    public void finishOnMainThread(OnStringResponseListener onResponseListener, String bean, Exception e) {
        SwingUtilities.invokeLater(() -> {
            onResponseListener.onFinish(bean, e);
        });
    }

    public static void log(Object msg) {
        LoggerUtil.log(msg);
    }

    public static Connection getDrugDatabase() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:database\\drugDatabase.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
