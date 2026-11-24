package com.leetkob.publish.dto;

public record PublicationReceipt(
    int labId, String status, String publishedAt, String publishedBy, int submissionCount) {}
