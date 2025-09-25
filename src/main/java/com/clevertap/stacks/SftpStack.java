package com.clevertap.stacks;

import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.GatewayVpcEndpoint;
import software.amazon.awscdk.services.ec2.GatewayVpcEndpointAwsService;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.transfer.CfnServer;
import software.amazon.awscdk.services.transfer.CfnUser;
import software.amazon.awscdk.services.transfer.CfnServer.EndpointDetailsProperty;
import software.amazon.awscdk.services.transfer.CfnUser.HomeDirectoryMapEntryProperty;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.logs.LogStream;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class SftpStack extends Stack {
    private final Bucket sftpBucket;
    private final CfnServer sftpServer;
    private final CfnUser sftpUser;
    private final Role transferRole;
    private final Vpc vpc;
    private final SecurityGroup sftpSecurityGroup;

    public SftpStack(final Construct scope, final String id) {
        this(scope, id, null, SftpStackProps.builder().build());
    }

    public SftpStack(final Construct scope, final String id, final StackProps props) {
        this(scope, id, props, SftpStackProps.builder().build());
    }

    public SftpStack(final Construct scope, final String id, final StackProps props, final SftpStackProps sftpProps) {
        super(scope, id, props);

        // Create VPC with public and private subnets
        this.vpc = Vpc.Builder.create(this, "SftpVpc")
                .ipAddresses(software.amazon.awscdk.services.ec2.IpAddresses.cidr(sftpProps.getVpcCidr()))
                .maxAzs(2)
                .natGateways(1)
                .subnetConfiguration(Arrays.asList(
                    software.amazon.awscdk.services.ec2.SubnetConfiguration.builder()
                            .name("Public")
                            .subnetType(SubnetType.PUBLIC)
                            .cidrMask(24)
                            .build(),
                    software.amazon.awscdk.services.ec2.SubnetConfiguration.builder()
                            .name("Private")
                            .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                            .cidrMask(24)
                            .build()
                ))
                .build();

        // Create security group for SFTP server
        this.sftpSecurityGroup = SecurityGroup.Builder.create(this, "SftpSecurityGroup")
                .vpc(this.vpc)
                .description("Security group for SFTP server")
                .allowAllOutbound(true)
                .build();

        // Allow SFTP traffic (port 22) from specific IP addresses only
        for (String allowedIP : sftpProps.getAllowedIPs()) {
            this.sftpSecurityGroup.addIngressRule(
                    software.amazon.awscdk.services.ec2.Peer.ipv4(allowedIP),
                    Port.tcp(22),
                    "Allow SFTP access from " + allowedIP
            );
        }

        // Create VPC endpoints for S3 if enabled
        if (sftpProps.isEnableVpcEndpoint()) {
            // S3 Gateway VPC Endpoint
            GatewayVpcEndpoint.Builder.create(this, "S3VpcEndpoint")
                    .vpc(this.vpc)
                    .service(GatewayVpcEndpointAwsService.S3)
                    .build();
        }

        // Create S3 bucket for SFTP file storage with encryption
        this.sftpBucket = Bucket.Builder.create(this, "SftpStorageBucket")
                .bucketName(sftpProps.getS3BucketName()) // Let CDK auto-generate if null
                .encryption(BucketEncryption.S3_MANAGED)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .versioned(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Create CloudWatch Log Group for Transfer Family logging
        final LogGroup transferLogGroup = LogGroup.Builder.create(this, "TransferLogGroup")
                .logGroupName("/aws/transfer/" + getStackName())
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // Create Log Stream for the server
        LogStream.Builder.create(this, "TransferLogStream")
                .logGroup(transferLogGroup)
                .logStreamName("server-logs")
                .build();

        // Create IAM role for AWS Transfer Family service
        this.transferRole = Role.Builder.create(this, "TransferServiceRole")
                .assumedBy(new ServicePrincipal("transfer.amazonaws.com"))
                .description("Role for AWS Transfer Family SFTP server")
                .build();

        // Grant the transfer role permissions to access the S3 bucket
        this.sftpBucket.grantReadWrite(this.transferRole);

        // Add additional permissions for Transfer Family
        this.transferRole.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                    "s3:ListBucket",
                    "s3:GetBucketLocation",
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject",
                    "s3:GetObjectVersion",
                    "s3:PutObjectAcl",
                    "s3:GetObjectAcl"
                ))
                .resources(Arrays.asList(
                    this.sftpBucket.getBucketArn(),
                    this.sftpBucket.getBucketArn() + "/*"
                ))
                .build());

        // Add CloudWatch Logs permissions
        this.transferRole.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(Arrays.asList(
                    "logs:CreateLogGroup",
                    "logs:CreateLogStream",
                    "logs:PutLogEvents",
                    "logs:DescribeLogGroups",
                    "logs:DescribeLogStreams"
                ))
                .resources(Arrays.asList(
                    transferLogGroup.getLogGroupArn(),
                    transferLogGroup.getLogGroupArn() + ":*"
                ))
                .build());

        // Create Elastic IPs for internet-facing VPC endpoint
        List<software.amazon.awscdk.services.ec2.CfnEIP> elasticIPs = new ArrayList<>();
        for (int i = 0; i < this.vpc.getPublicSubnets().size(); i++) {
            software.amazon.awscdk.services.ec2.CfnEIP elasticIP = software.amazon.awscdk.services.ec2.CfnEIP.Builder.create(this, "ElasticIP" + i)
                    .domain("vpc")
                    .build();
            elasticIPs.add(elasticIP);
        }

        // Create AWS Transfer Family SFTP Server in VPC with internet-facing access
        this.sftpServer = CfnServer.Builder.create(this, "SftpServer")
                .endpointType("VPC")
                .identityProviderType("SERVICE_MANAGED")
                .protocols(Arrays.asList("SFTP"))
                .domain("S3")
                .loggingRole(this.transferRole.getRoleArn())
                .endpointDetails(EndpointDetailsProperty.builder()
                    .vpcId(this.vpc.getVpcId())
                    .subnetIds(this.vpc.getPublicSubnets().stream()
                            .map(subnet -> subnet.getSubnetId())
                            .collect(java.util.stream.Collectors.toList()))
                    .securityGroupIds(Arrays.asList(this.sftpSecurityGroup.getSecurityGroupId()))
                    .addressAllocationIds(elasticIPs.stream()
                            .map(eip -> eip.getAttrAllocationId())
                            .collect(java.util.stream.Collectors.toList()))
                    .build())
                .build();


        // Create SFTP User with SSH key authentication
        this.sftpUser = CfnUser.Builder.create(this, "SftpUser")
                .serverId(this.sftpServer.getAttrServerId())
                .userName(sftpProps.getSftpUserName())
                .role(this.transferRole.getRoleArn())
                .homeDirectory("/" + this.sftpBucket.getBucketName())
                .homeDirectoryType("LOGICAL")
                .homeDirectoryMappings(Arrays.asList(
                    HomeDirectoryMapEntryProperty.builder()
                        .entry("/")
                        .target("/" + this.sftpBucket.getBucketName())
                        .build()
                ))
                .sshPublicKeys(Arrays.asList(sftpProps.getSshPublicKey()))
                .build();

        // Outputs
        CfnOutput.Builder.create(this, "SftpServerEndpoint")
                .description("SFTP Server Endpoint")
                .value(this.sftpServer.getAttrServerId() + ".server.transfer." + getRegion() + ".amazonaws.com")
                .build();

        CfnOutput.Builder.create(this, "SftpServerId")
                .description("SFTP Server ID")
                .value(this.sftpServer.getAttrServerId())
                .build();

        CfnOutput.Builder.create(this, "SftpUserName")
                .description("SFTP Username")
                .value(sftpProps.getSftpUserName())
                .build();

        CfnOutput.Builder.create(this, "S3BucketName")
                .description("S3 Bucket for SFTP Storage")
                .value(this.sftpBucket.getBucketName())
                .build();

        CfnOutput.Builder.create(this, "VpcId")
                .description("VPC ID")
                .value(this.vpc.getVpcId())
                .build();
    }

    // Getters for accessing resources from other stacks or constructs
    public Bucket getSftpBucket() {
        return this.sftpBucket;
    }

    public CfnServer getSftpServer() {
        return this.sftpServer;
    }

    public CfnUser getSftpUser() {
        return this.sftpUser;
    }

    public Role getTransferRole() {
        return this.transferRole;
    }

    public Vpc getVpc() {
        return this.vpc;
    }

    public SecurityGroup getSftpSecurityGroup() {
        return this.sftpSecurityGroup;
    }

    public String getSftpServerEndpoint() {
        return this.sftpServer.getAttrServerId() + ".server.transfer." + getRegion() + ".amazonaws.com";
    }
}
