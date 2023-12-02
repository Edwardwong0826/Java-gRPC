package com.wong.grpc.serializer;

import com.google.protobuf.util.JsonFormat;
import com.wong.grpc.pb.Laptop;

import java.io.*;

public class Serializer {

    public void WriteBinaryFile(Laptop laptop, String filename) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filename);
        laptop.writeTo(outputStream);
        outputStream.close();
    }

    public Laptop ReadBinaryFile(String filename) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filename);
        Laptop laptop = Laptop.parseFrom(fileInputStream);
        fileInputStream.close();
        return laptop;
    }

    public void WriteJsonFile(Laptop laptop, String fileName) throws IOException {
        JsonFormat.Printer printer = JsonFormat.printer().includingDefaultValueFields()
                .preservingProtoFieldNames();

        String jsonString = printer.print(laptop);

        FileOutputStream outputStream = new FileOutputStream(fileName);
        outputStream.write(jsonString.getBytes());
        outputStream.close();

    }



}