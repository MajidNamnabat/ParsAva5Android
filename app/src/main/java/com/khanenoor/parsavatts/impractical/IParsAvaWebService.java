package com.khanenoor.parsavatts.impractical;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IParsAvaWebService {
    @Headers({
            "Content-Type: text/xml; charset=utf-8",
            "SOAPAction:http://tempuri.org/IAuthService/RegisterV2"
    })
    @POST("AuthService.svc/CheckService")
    Call<ResponseBody> RegisterV2(@Body RequestBody body );

    @Headers({
            "Content-Type: text/xml; charset=utf-8",
            "SOAPAction:http://tempuri.org/IAuthService/IsVersionValid"
    })
    @POST("AuthService.svc/CheckService")
    Call<ResponseBody> IsVersionValid(@Body RequestBody body);

    @Headers({
                "Content-Type: text/xml; charset=utf-8",
            "SOAPAction:http://tempuri.org/IAuthService/IsFileSignValid"
    })
    @POST("AuthService.svc/CheckService")
    Call<ResponseBody> IsFileSignValid(@Body RequestBody body);

    @Headers({
        "Content-Type: text/xml; charset=utf-8",
            "SOAPAction:http://tempuri.org/IAuthService/GetProductKey"
    })
    @POST("AuthService.svc/CheckService")
    Call<ResponseBody> GetProductKey(@Body RequestBody body);

    @Headers({
            "Content-Type: text/xml; charset=utf-8",
            "SOAPAction:http://tempuri.org/IAuthService/GetLicenseKey"
    })
    @POST("AuthService.svc/CheckService")
    Call<ResponseBody> GetLicenseKey(@Body RequestBody body);

    @Headers({
            "Content-Type: text/xml; charset=utf-8",
            "SOAPAction:http://tempuri.org/IAuthService/GetLicenseKeyV2"
    })
    @POST("AuthService.svc/CheckService")
    Call<ResponseBody> GetLicenseKeyV2(@Body RequestBody body);
}
