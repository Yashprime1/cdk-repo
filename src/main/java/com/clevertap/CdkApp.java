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
                .sshPublicKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDT95yFKUz5bqZYdK7Eb4S3yHHEekLUh55snZAFK+cW89c4pngV1ow69yBmYKxUrz4EN8+Aw7fYxhrWIus/XyQnx5tLTrUDmUeqZemT0hO4wnPImP8ArDUr9WYKRxKaNH0d1ie+asETnFzPt+SFB9YxhlMzTXlzEIJgd9l/iD7kCTAO1/jKT6ohNM9VaQubrDXrO6jQWYPreY+kdpjuT8XrkrDy6w1zJZxev2ulvexZV999b9XE3RLHGjiW4FPf47ZMAljz1Q06fwfw0rf8CvK3hiyvAbvOCOqnCzF6V66/d2CDj9Q3lCI9jIWz3yTWFGJpzyhEC7wtyAXxD2ySFynHgvKCB2WrGbfvxdQkYQu6FnG0ZKN6Uw3ur9GYOvnAk3VCNvxMDHrnS3AeH4v2EcyUI0GSY+zhhTuKX8ttcts7zuAdQcoJkmzw3WrDkhNkbgG6GzhwInm7T2vbYyD2+6ogmVqHx+m4EKz9FprL0ZvcwB1TsJyLpZCIobl+em7oYSlejZpzUiAXM+3x/Te0aphrgBklXjc1OWq3pqhnZ/IZ6tU0JA6uac2y6BcIW5awGHRP2z/aw0Q6bSegxhcgjf71iTfM64Cz1HT7ucAc896Hx0ed1vrscOjPmkoKCMJEC+a/QyOUZIGARK923GUgCzXA+b6k0m1qC7yiav1HpSnBHw== yashdeep@Yashdeep-MacBook-Pro") // Replace with your actual SSH public key
                .s3BucketName("cdk-sftp-storage-bucket-unique") // Unique bucket name
                .vpcCidr("10.0.0.0/16")
                .availabilityZones(new String[]{"us-east-1a", "us-east-1b"})
                .enableVpcEndpoint(true)
                .environment("dev")
                .build();

        new SftpStack(app, "CdkSftpStack", StackProps.builder()
                // If you don't specify 'env', this stack will be environment-agnostic.
                // Account/Region-dependent features and context lookups will not work,
                // but a single synthesized template can be deployed anywhere.

                // Uncomment the next block to specialize this stack for the AWS Account
                // and Region that are implied by the current CLI configuration.
                /*
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                */

                // Uncomment the next block if you know exactly what Account and Region you
                // want to deploy the stack to.
                /*
                .env(Environment.builder()
                        .account("123456789012")
                        .region("us-east-1")
                        .build())
                */

                // For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html
                .build(), sftpProps);

        app.synth();
    }
}

