package vn.edu.hcmus.leetkob.publish;

public record PublicationReceipt(
    int labId, String status, String publishedAt, String publishedBy, int submissionCount) {}
