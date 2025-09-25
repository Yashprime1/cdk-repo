package com.clevertap.stacks;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SftpStackProps {
    private final String sshPublicKey; 
    private final String s3BucketName;
    private final String vpcCidr;  
    private final String[] availabilityZones;
    private final boolean enableVpcEndpoint;
    private final String environment;
    private final String sftpUserName;
    private final String[] allowedIPs;
}