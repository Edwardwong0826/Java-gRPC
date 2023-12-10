package com.wong.grpc.service;

public interface RatingStore {
    Rating Add(String laptopID, double score);
}
