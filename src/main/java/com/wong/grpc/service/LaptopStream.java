package com.wong.grpc.service;

import com.wong.grpc.pb.Laptop;

public interface LaptopStream {
    void Send(Laptop laptop);
}
