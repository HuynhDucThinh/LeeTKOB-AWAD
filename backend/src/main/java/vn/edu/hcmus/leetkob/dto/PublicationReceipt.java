package vn.edu.hcmus.leetkob.dto;

public record PublicationReceipt(
    int labId, String status, String publishedAt, String publishedBy, int submissionCount) {}
