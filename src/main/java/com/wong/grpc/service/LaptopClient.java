package com.wong.grpc.service;

import com.wong.grpc.pb.CreateLaptopRequest;
import com.wong.grpc.pb.CreateLaptopResponse;
import com.wong.grpc.pb.Laptop;
import com.wong.grpc.pb.LaptopServiceGrpc;
import com.wong.grpc.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LaptopClient {

    private static final Logger logger = Logger.getLogger(LaptopServer.class.getName());

    private final ManagedChannel channel;
    // this blocking stub is to call the unary RPC
    private final LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub;

    public LaptopClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void createLaptop(Laptop laptop) {
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        CreateLaptopResponse response = CreateLaptopResponse.getDefaultInstance();

        try {
            response = blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).createLaptop(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                // not a big deal
                logger.info("laptop ID already exists");
                return;
            }
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return;
        }

        logger.info("laptop created with ID: " + response.getId());
    }

    public static void main(String[] args) throws InterruptedException {
        LaptopClient laptopClient = new LaptopClient("0.0.0.0", 8080);
        Generator generator = new Generator();
        Laptop laptop = generator.NewLaptop();

        try {
            laptopClient.createLaptop(laptop);
        } finally {
            laptopClient.shutdown();
        }
    }

}
