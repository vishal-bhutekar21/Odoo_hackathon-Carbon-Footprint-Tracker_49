package com.chaitany.carbonview;

public class UploadItem {
    private String fileName, date, fileUrl, mobile;

    public UploadItem() {}

    public UploadItem(String fileName, String date, String fileUrl, String mobile) {
        this.fileName = fileName;
        this.date = date;
        this.fileUrl = fileUrl;
        this.mobile = mobile;
    }

    public String getFileName() { return fileName; }
    public String getDate() { return date; }
    public String getFileUrl() { return fileUrl; }
    public String getMobile() { return mobile; }
}
