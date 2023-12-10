package com.wong.grpc.service;

import com.google.protobuf.ByteString;
import com.wong.grpc.pb.*;
import com.wong.grpc.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LaptopClient {

    private static final Logger logger = Logger.getLogger(LaptopServer.class.getName());

    private final ManagedChannel channel;
    // this blocking stub is to call the unary RPC
    private final LaptopServiceGrpc.LaptopServiceBlockingStub blockingStub;
    // we cannot use blocking stub to call the client streaming RPC, bidirectional-streaming RPC instead need to use asynchronous stub
    private final LaptopServiceGrpc.LaptopServiceStub asyncStub;


    public LaptopClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        blockingStub = LaptopServiceGrpc.newBlockingStub(channel);
        asyncStub = LaptopServiceGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void createLaptop(Laptop laptop) {
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();
        CreateLaptopResponse response = CreateLaptopResponse.getDefaultInstance();

        try {
            // send the RateLaptopRequest defined in proto file, noted this will call via gRPC stub, and this gRPC stub will call the underlying service class that implements generated gRPC interface methods
            // (doesn't matter on which programming language, example Java gRPC and Go gRPC can interchange via the same define protobuf request)
            // this RateLaptopRequest is kind of like JSON in REST to exchange in between API
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

        // send the RateLaptopRequest defined in proto file, noted this will call via gRPC stub, and this gRPC stub will call the underlying service class that implements generated gRPC interface methods
        // (doesn't matter on which programming language, example Java gRPC and Go gRPC can interchange via the same define protobuf request)
        // this RateLaptopRequest is kind of like JSON in REST to exchange in between API
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

    public void uploadImage(String laptopID, String imagePath) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<UploadImageRequest> requestObserver = asyncStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .uploadImage(new StreamObserver<UploadImageResponse>() {
                    @Override
                    public void onNext(UploadImageResponse response) {
                        logger.info("receive response:\n" + response);
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.log(Level.SEVERE, "upload failed: " + t);
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("image uploaded");
                        finishLatch.countDown();
                    }
                });

        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(imagePath);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "cannot read image file: " + e.getMessage());
            return;
        }

        String imageType = imagePath.substring(imagePath.lastIndexOf("."));
        ImageInfo info = ImageInfo.newBuilder().setLaptopId(laptopID).setImageType(imageType).build();
        UploadImageRequest request = UploadImageRequest.newBuilder().setInfo(info).build();

        try {
            // send the RateLaptopRequest defined in proto file, noted this will call via gRPC stub, and this gRPC stub will call the underlying service class that implements generated gRPC interface methods
            // (doesn't matter on which programming language, example Java gRPC and Go gRPC can interchange via the same define protobuf request)
            // this RateLaptopRequest is kind of like JSON in REST to exchange in between API
            requestObserver.onNext(request);
            logger.info("sent image info:\n" + info);

            byte[] buffer = new byte[1024];
            while (true) {
                int n = fileInputStream.read(buffer);
                if (n <= 0) {
                    break;
                }

                if (finishLatch.getCount() == 0) {
                    return;
                }

                request = UploadImageRequest.newBuilder()
                        .setChunkData(ByteString.copyFrom(buffer, 0, n))
                        .build();
                requestObserver.onNext(request);
                logger.info("sent image chunk with size: " + n);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "unexpected error: " + e.getMessage());
            requestObserver.onError(e);
            return;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            logger.warning("request cannot finish within 1 minute");
        }
    }

    public void rateLaptop(String[] laptopIDs, double[] scores) throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RateLaptopRequest> requestObserver = asyncStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .rateLaptop(new StreamObserver<RateLaptopResponse>() {
                    @Override
                    public void onNext(RateLaptopResponse response) {
                        logger.info("laptop rated: id = " + response.getLaptopId() +
                                ", count = " + response.getRatedCount() +
                                ", average = " + response.getAverageScore());
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.log(Level.SEVERE, "rate laptop failed: " + t.getMessage());
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("rate laptop completed");
                        finishLatch.countDown();
                    }
                });

        int n = laptopIDs.length;
        try {
            for (int i = 0; i < n; i++) {
                RateLaptopRequest request = RateLaptopRequest.newBuilder()
                        .setLaptopId(laptopIDs[i])
                        .setScore(scores[i])
                        .build();
                // send the RateLaptopRequest defined in proto file, noted this will call via gRPC stub, and this gRPC stub will call the underlying service class that implements generated gRPC interface methods
                // (doesn't matter on which programming language, example Java gRPC and Go gRPC can interchange via the same define protobuf request)
                // this RateLaptopRequest is kind of like JSON in REST to exchange in between API
                requestObserver.onNext(request);
                logger.info("sent rate-laptop request: id = " + request.getLaptopId() + ", score = " + request.getScore());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "unexpected error: " + e.getMessage());
            requestObserver.onError(e);
            return;
        }

        requestObserver.onCompleted();
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            logger.warning("request cannot finish within 1 minute");
        }
    }

    public static void testCreateLaptop(LaptopClient client, Generator generator) {
        Laptop laptop = generator.NewLaptop();
        client.createLaptop(laptop);
    }

    public static void testSearchLaptop(LaptopClient client, Generator generator) {
        for (int i = 0; i < 10; i++) {
            Laptop laptop = generator.NewLaptop();
            client.createLaptop(laptop);
        }

        Memory minRam = Memory.newBuilder()
                .setValue(8)
                .setUnit(Memory.Unit.GIGABYTE)
                .build();
        Filter filter = Filter.newBuilder()
                .setMaxPriceUsd(3000)
                .setMinCpuCores(4)
                .setMinCpuGhz(2.5)
                .setMinRam(minRam)
                .build();
        client.searchLaptop(filter);
    }

    public static void testUploadImage(LaptopClient client, Generator generator) throws InterruptedException {
        Laptop laptop = generator.NewLaptop();
        client.createLaptop(laptop);
        client.uploadImage(laptop.getId(), "tmp/laptop.jpg");
    }

    public static void testRateLaptop(LaptopClient client, Generator generator) throws InterruptedException {
        int n = 3;
        String[] laptopIDs = new String[n];

        for (int i = 0; i < n; i++) {
            Laptop laptop = generator.NewLaptop();
            laptopIDs[i] = laptop.getId();
            client.createLaptop(laptop);
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            logger.info("rate laptop (y/n)? ");
            String answer = scanner.nextLine();
            if (answer.toLowerCase().trim().equals("n")) {
                break;
            }

            double[] scores = new double[n];
            for (int i = 0; i < n; i++) {
                scores[i] = generator.NewLaptopScore();
            }

            client.rateLaptop(laptopIDs, scores);
        }
    }




    public static void main(String[] args) throws InterruptedException {
        LaptopClient laptopClient = new LaptopClient("0.0.0.0", 8080);
        Generator generator = new Generator();

        try {

            // put in the method to for the client to test it out the feature
            //testCreateLaptop(laptopClient, generator);
            //testSearchLaptop(laptopClient,generator);
            //testUploadImage(laptopClient, generator);
            testRateLaptop(laptopClient, generator);

        } finally {
            laptopClient.shutdown();
        }
    }

}
