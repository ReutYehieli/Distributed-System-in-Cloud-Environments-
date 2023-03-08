package com.reut.yehieli.common;

public class S3Key {

    private static String prefix = "s3://";
    private static int prefixLength = prefix.length();
    private String bucket;
    private String key;

    public static S3Key valueOf(String value) {
        if (value.startsWith(prefix)) {
            int slashIndex = value.indexOf('/', prefixLength);
            return new S3Key(value.substring(prefixLength, slashIndex), value.substring(slashIndex + 1));
        } else {
            System.exit(Helper.wrongS3KeyExitCode);
            return null;
        }
    }

    public S3Key(String bucket, String key) {
        this.bucket = bucket;
        this.key = key;
    }

    public String getBucket() {
        return bucket;
    }
    public String getKey() {
        return key;
    }
    @Override
    public String toString() {
        return "s3://" + bucket + "/" + key;
    }
}
