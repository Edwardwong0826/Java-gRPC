package com.wong.grpc.service;

import com.wong.grpc.pb.Filter;
import com.wong.grpc.pb.Laptop;
import io.grpc.Context;

public interface LaptopStore {
    void Save(Laptop laptop) throws Exception;
    Laptop Find(String id);

    void Search(Context ctx, Filter filter, LaptopStream stream);
}


