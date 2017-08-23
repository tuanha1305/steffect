package com.facedemo.com.facesdkbuild.request;

import com.facebeauty.com.beautysdk.domain.Body;
import com.facebeauty.com.beautysdk.domain.HeadBean;

/**
 * Created by wangdi on 2017/8/17.
 */

public class STLicenseBean {
    HeadBean head;
    Body body;

    public Body getBody() {
        return body;
    }

    public HeadBean getHead() {
        return head;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public void setHead(HeadBean head) {
        this.head = head;
    }
}
