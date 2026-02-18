package com.khanenoor.parsavatts.impractical;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "s:Envelope", strict = false)
@Namespace(prefix = "s",reference = "http://schemas.xmlsoap.org/soap/envelope/")
public class GetLicenseKeyElement {


    //@Element(name = "s:Body", required = true)
    @Element(name = "GetLicenseKeyResult", required = false)
    @Path("s:Body/GetLicenseKeyResponse")
    private String LicenseKeyBody;

    public String getLicenseKeyBody(){return LicenseKeyBody;}
    public void setLicenseKeyBody(String lic){LicenseKeyBody=lic;}
    //@Root(name = "s:Body", strict = false)

    public class GetLicenseKeyBody {
        @Element(name = "GetLicenseKeyResponse", required = false)
        @Namespace(reference = "http://tempuri.org/")
        public GetLicenseKeyResponse getLicenseKeyResponse;
    }

    //@Root(name = "GetLicenseKeyResponse", strict = false)
    //@Namespace(reference = "http://tempuri.org/")
    public class GetLicenseKeyResponse {
        @Element(name = "GetLicenseKeyResult",required = false)
        public String result;
    };

}
