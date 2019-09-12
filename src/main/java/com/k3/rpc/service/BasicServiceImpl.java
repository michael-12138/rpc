package com.k3.rpc.service;

import com.k3.rpc.RpcService;

/**
 * Created on 2019-09-10 10:52
 * Author : Michael.
 */
@RpcService(BasicService.class)
public class BasicServiceImpl implements BasicService {

    @Override
    public String ping() {
        return "pong";
    }

}
