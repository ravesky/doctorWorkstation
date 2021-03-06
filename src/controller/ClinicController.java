package controller;

import Utils.StringUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.ApiUrl;
import model.Model;
import model.OnStringResponseListener;
import model.bean.*;

import javax.swing.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClinicController {
    List<ClinicRegisterBean> clinicRegisterList;
    HashMap<String,ClinicRegisterBean> clinicRegisterListMap;
    private Gson gson = Model.getGson();
    private Type type = new Type() {
    };
    private boolean duplicated;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ClinicController() {
    }

    public List<ClinicRegisterBean> getClinicRegisterList() {
        return clinicRegisterList;
    }

    public void setClinicRegisterList(List<ClinicRegisterBean> clinicRegisterList) {
        this.clinicRegisterList = clinicRegisterList;
        if (clinicRegisterList!=null) {
            if (clinicRegisterListMap!=null) {
                clinicRegisterListMap.clear();
            }else
                clinicRegisterListMap=new HashMap<>();
            for (int i = 0; i <clinicRegisterList.size() ; i++) {
                clinicRegisterListMap.put(clinicRegisterList.get(i).getObjectId(),clinicRegisterList.get(i));
            }
        }
    }

    public HashMap<String, ClinicRegisterBean> getClinicRegisterListMap() {
        return clinicRegisterListMap;
    }

    public void setClinicRegisterListMap(HashMap<String, ClinicRegisterBean> clinicRegisterListMap) {
        this.clinicRegisterListMap = clinicRegisterListMap;
    }

    public void newPatientInfo(PatientInfo patientInfo, SimpleListener simpleListener) {
        Model model=new Model();
        model.postData(ApiUrl.Post.PatientInfo_URL, patientInfo, new OnStringResponseListener() {
            @Override
            public void onFinish(String responseBean, Exception e) {
                if (e == null) {
                    try {
                        if (responseBean.contains("objectId")) {

                            simpleListener.done(gson.fromJson(responseBean, SucceedInfo.class));

                        } else {
                            if (responseBean.contains("code") && responseBean.contains("error")) {
                                simpleListener.fail(gson.fromJson(responseBean, ErrInfo.class));
                            } else
                                simpleListener.fail(responseBean);
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        simpleListener.fail(e1.getMessage());
                    }
                } else {
                    simpleListener.fail(e.getMessage());
                }
            }
        });
    }

    public void newRegister(ClinicRegister clinicRegister, SimpleListener simpleListener){
        getQueueNumber(clinicRegister, new SimpleListener<Integer>() {
            @Override
            public void done(Integer data) {
                if (data>10){
                    simpleListener.fail("该医生已满号，请更换医生");
                    return;
                }
                else {
                    int i=-1;
                    if (duplicated){
                        i= JOptionPane.showConfirmDialog(null,"该患者当天已挂号，确认继续挂号？");
                    }
                    if (i==0||i==-1){
                        clinicRegister.setQueueNumber(data);
                        doNewRegister(clinicRegister,simpleListener);
                    }else {
                        simpleListener.fail("取消挂号");
                    }

                }
            }

            @Override
            public void fail(ErrInfo errInfo) {
                simpleListener.fail(errInfo);
            }

            @Override
            public void fail(String s) {
                simpleListener.fail(s);
            }
        });

    }

    private void doNewRegister(ClinicRegister clinicRegister, SimpleListener simpleListener) {
        String date=Controller.getToday();
        Model model=new Model();
        model.postData(ApiUrl.Post.REGISTER_Info+date, clinicRegister, new OnStringResponseListener() {
            @Override
            public void onFinish(String responseBean, Exception e) {
                if (e == null) {
                    try {
                        if (!responseBean.contains("code")&&!responseBean.contains("error")) {
                            simpleListener.done("挂号成功，排队号 "+clinicRegister.getQueueNumber()+"\n"+responseBean);
                        }else {
                            simpleListener.fail(responseBean);
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } else {
                    simpleListener.fail(e.getMessage());
                }
            }
        });
    }

    public void delRegister(String id, SimpleListener simpleListener) {
        Model model=new Model();
        String date=Controller.getToday();
        model.delData(ApiUrl.Post.REGISTER_Info+date, id, new OnStringResponseListener() {
            @Override
            public void onFinish(String responseBean, Exception e) {
                if (e == null) {
                    simpleListener.done(responseBean);

                } else {
                    simpleListener.fail(e.getMessage());
                }
            }
        });
    }

    public ClinicRegisterBean findLocalRegister(String objectId){
        if (clinicRegisterListMap!=null&&!StringUtil.isEmpty(objectId)){
//            print("do find "+objectId);
            return clinicRegisterListMap.get(objectId);
        }else
        {
//            print("do not find ");
            return null;
        }
    }

    public List<ClinicRegisterBean> findLocalRegisterByName(String name){
        if (clinicRegisterList!=null&&!StringUtil.isEmpty(name)){
            print("do find "+name);
            try {
                List<ClinicRegisterBean> result=new ArrayList<>();
                List<PatientInfoBean> registerBeanList=Controller.getPatientInfoBeanList();
                for (int i = 0; i <registerBeanList.size() ; i++) {
                    Controller.getPatientInfoBeanList();
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }else
        {
            print("do not find ");
            return null;
        }
    }

    public static void print(Object o){
        System.out.println("--------clinicController----------");
        System.out.println(o);
    }
    public void getRegisterList(SimpleListener simpleListener) {
        String date=Controller.getToday();
        Model model=new Model();

        model.getData(ApiUrl.Post.REGISTER_Info+date, null, new OnStringResponseListener() {
            @Override
            public void onFinish(String responseBean, Exception e) {
                if (e == null) {
                    try {
                        if (!responseBean.contains("code")&& !responseBean.contains("error")) {
                            Type type = new TypeToken<ResultBean<ClinicRegisterBean>>() {
                            }.getType();
                            ResultBean<ClinicRegisterBean> resultBean=gson.fromJson(responseBean,type);
                            List<ClinicRegisterBean> clinicRegisterBeans=resultBean.getResults();
                            setClinicRegisterList(clinicRegisterBeans);
                            simpleListener.done(clinicRegisterBeans);
                        }else {
                                simpleListener.done(new ArrayList<>());
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        simpleListener.fail(e1.getMessage());
                    }
                } else {
                    simpleListener.fail(e.getMessage());
                }
            }
        });
    }


    public void getQueueNumber(ClinicRegister clinicRegister, SimpleListener simpleListener) {
        String date=Controller.getToday();
        Model model=new Model();

        LinkedHashMap<String,String> map=new LinkedHashMap<>();
        String doctorID=clinicRegister.getDoctorID();
        String patientID=clinicRegister.getPatientID();
        map.put("doctorID",doctorID);
        model.getData(ApiUrl.Post.REGISTER_Info+date, map, new OnStringResponseListener() {
            @Override
            public void onFinish(String responseBean, Exception e) {
                if (e == null) {
                    try {
                        if (!responseBean.contains("code")&& !responseBean.contains("error")) {
                            Type type = new TypeToken<ResultBean<ClinicRegisterBean>>() {
                            }.getType();
                            ResultBean<ClinicRegisterBean> resultBean=gson.fromJson(responseBean,type);
                            List<ClinicRegisterBean> clinicRegisterBeans=resultBean.getResults();
                            Integer count=0;
                            duplicated=false;
                            for (int i = 0; i < clinicRegisterBeans.size(); i++) {
                                ClinicRegisterBean bean=clinicRegisterBeans.get(i);
                                if (bean.getDoctorID().equals(doctorID)){
                                    count++;
                                }
                                if (bean.getPatientID().equals(patientID)){
                                    duplicated=true;
                                }
                            }
                            simpleListener.done(count+1);
                        }else {
                            if (responseBean.contains("object not found")){
                                simpleListener.done(1);
                            }else
                            simpleListener.fail(responseBean);
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        simpleListener.fail(e1.getMessage());
                    }
                } else {
                    simpleListener.fail(e.getMessage());
                }
            }
        });
    }
}

