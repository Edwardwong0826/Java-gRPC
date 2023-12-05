package com.wong.grpc.service;

import com.wong.grpc.pb.*;
import com.wong.grpc.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Iterator;
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

    public void searchLaptop(Filter filter) {
        logger.info("search started");

        SearchLaptopRequest request = SearchLaptopRequest.newBuilder().setFilter(filter).build();

        try {
            Iterator<SearchLaptopResponse> iterator = blockingStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .searchLaptop(request);

            while (iterator.hasNext()) {
                SearchLaptopResponse response = iterator.next();
                Laptop laptop = response.getLaptop();
                logger.info("- found: " + laptop.getId());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "request failed: " + e.getMessage());
            return;
        }

        logger.info("search completed");
    }

    public static void main(String[] args) throws InterruptedException {
        LaptopClient laptopClient = new LaptopClient("0.0.0.0", 8080);
        Generator generator = new Generator();

        try {
            for (int i=0; i<10; i++){
                Laptop laptop = generator.NewLaptop();
                laptopClient.createLaptop(laptop);
            }

            Memory ram = Memory.newBuilder().setValue(8).setUnit(Memory.Unit.GIGABYTE).build();
            Filter filter = Filter.newBuilder().setMaxPriceUsd(3000).setMinCpuCores(4).setMinCpuGhz(2.5).setMinRam(ram).build();

            laptopClient.searchLaptop(filter);

        } finally {
            laptopClient.shutdown();
        }
    }

}
