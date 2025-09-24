package com.clevertap.stacks;


public class SftpStackProps {
    private final String sftpUserName;
    private final String sshPublicKey;
    private final String s3BucketName;
    private final String vpcCidr;
    private final String[] availabilityZones;
    private final boolean enableVpcEndpoint;
    private final String environment;

    private SftpStackProps(Builder builder) {
        super();
        this.sftpUserName = builder.sftpUserName;
        this.sshPublicKey = builder.sshPublicKey;
        this.s3BucketName = builder.s3BucketName;
        this.vpcCidr = builder.vpcCidr;
        this.availabilityZones = builder.availabilityZones;
        this.enableVpcEndpoint = builder.enableVpcEndpoint;
        this.environment = builder.environment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSftpUserName() {
        return sftpUserName;
    }

    public String getSshPublicKey() {
        return sshPublicKey;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public String getVpcCidr() {
        return vpcCidr;
    }

    public String[] getAvailabilityZones() {
        return availabilityZones;
    }

    public boolean isEnableVpcEndpoint() {
        return enableVpcEndpoint;
    }

    public String getEnvironment() {
        return environment;
    }

    public static class Builder {
        private String sftpUserName = "sftpuser";
        private String sshPublicKey;
        private String s3BucketName;
        private String vpcCidr = "10.0.0.0/16";
        private String[] availabilityZones = {"us-east-1a", "us-east-1b"};
        private boolean enableVpcEndpoint = true;
        private String environment = "dev";

        public Builder sftpUserName(String sftpUserName) {
            this.sftpUserName = sftpUserName;
            return this;
        }

        public Builder sshPublicKey(String sshPublicKey) {
            this.sshPublicKey = sshPublicKey;
            return this;
        }

        public Builder s3BucketName(String s3BucketName) {
            this.s3BucketName = s3BucketName;
            return this;
        }

        public Builder vpcCidr(String vpcCidr) {
            this.vpcCidr = vpcCidr;
            return this;
        }

        public Builder availabilityZones(String[] availabilityZones) {
            this.availabilityZones = availabilityZones;
            return this;
        }

        public Builder enableVpcEndpoint(boolean enableVpcEndpoint) {
            this.enableVpcEndpoint = enableVpcEndpoint;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public SftpStackProps build() {
            if (sshPublicKey == null || sshPublicKey.trim().isEmpty()) {
                throw new IllegalArgumentException("SSH public key is required");
            }
            return new SftpStackProps(this);
        }
    }
}