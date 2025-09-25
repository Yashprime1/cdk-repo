package com.clevertap;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

import com.clevertap.stacks.SftpStack;
import com.clevertap.stacks.SftpStackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        // Create SFTP stack props with configuration
        SftpStackProps sftpProps = SftpStackProps.builder()
                .sftpUserName("sftpuser")
                .sshPublicKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDAu2Z0kHypXXi26lJLxCNePInmixSgBf8GciyGvzf+lT9jjk+wtIPmnFnicZANFdSnZ2kS7YJ4uw3Lbizhk9d+foUYe1hF3YPOntjOqcR+RxcvilR/GcQ9YENHo0tWIkAJfkfiEzB0A+kXGcsnTOHU0qTUdVlP7JV8Zh4EYQDYfuOcEAsseU7+ABRpMm21QiUvqs/xL95xsNQ1SDUn6zWqmg8snQxgmoP4eu9JCX8EahbhnUZV6jft3lvEIvDq5n01enMEagNzSmKZuzI47Y0s2Q/eeCWrAypsiC5DbBreWh7OgMdOwiSJ8cPtKB76KDsC9TBVDLTzaUDtOxHO4Ef94Wq8viE4FVithboaK5l5MU93sJcFMhwd2J12Dxs8cPoNOUFqiMrSJBSLl3PpPQ9WJLox247qau36DEtHYZIVpXCV99WutMa6Y7zq3XbEuRHiT9JGcqcYOnLdMWI+vJkzSycpSkA4qbIkfwdjZZmVe4Q6sDB6YgrD01RJvifL7s0= yashdeep@Yashdeep-MacBook-Pro") 
                .s3BucketName("cdk-sftp-storage-bucket-unique") 
                .vpcCidr("10.0.0.0/16")
                .availabilityZones(new String[]{"us-east-1a", "us-east-1b"})
                .enableVpcEndpoint(true)
                .environment("dev")
                .allowedIPs(new String[]{
                    "223.181.61.44/32",  // Current public IP
                    "203.0.113.0/24",    
                    "198.51.100.0/24",   
                    "192.0.2.0/24",      
                    "10.0.0.0/8",        
                    "172.16.0.0/12",     
                    "192.168.0.0/16"     
                })
                .build();

        new SftpStack(app, "CdkSftpStack", StackProps.builder()
                .build(), sftpProps);

        app.synth();
    }
}

