package com.wong.grpc.serializer;

import com.wong.grpc.pb.Laptop;
import com.wong.grpc.sample.Generator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerializerTest {

    @Test
    public void writeAndReadBinaryFile() throws IOException {
        String binaryFile = "laptop.bin";
        Laptop laptop1 = new Generator().NewLaptop();

        Serializer serializer = new Serializer();
        serializer.WriteBinaryFile(laptop1, binaryFile);

        Laptop laptop2 = serializer.ReadBinaryFile(binaryFile);
        assertEquals(laptop1, laptop2);

    }
    // for serialize protobuf message to test success, not only test from java
    // we also can use the Laptop.bin generated in go gRPC project to generate json file and compare the values
    public static void main(String[] args) throws IOException {
        Serializer serializer = new Serializer();
        Laptop laptop = serializer.ReadBinaryFile("laptop.bin");
        serializer.WriteJsonFile(laptop, "laptop.json");
    }


}