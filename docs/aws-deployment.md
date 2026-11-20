# Deploying JanManch on AWS

Region used in the examples: `ap-south-1` (Mumbai). Take a screenshot at every
step; the report's Deployment Flow section needs them.

Set a budget alert first (Billing → Budgets) so unexpected charges are caught early.

## Architecture

```
Internet
   │
   ▼
Elastic Beanstalk (EC2, public subnet)  ── runs the JanManch jar + FFmpeg
   │            │             │
   ▼            ▼             ▼
RDS MySQL   ElastiCache    Kafka on EC2          S3 bucket
(private)   Redis (private) (private, Docker)    (private, videos + thumbnails)
```

## 1. VPC

- Create a VPC (`10.0.0.0/16`) with two public and two private subnets in two
  availability zones ("VPC and more" wizard does this in one step).
- Public subnets: the application. Private subnets: database, cache, Kafka.

## 2. Security groups

| Group | Inbound rule | Source |
|---|---|---|
| `janmanch-app-sg` | 80, 443 | Anywhere |
| `janmanch-db-sg` | 3306 | `janmanch-app-sg` only |
| `janmanch-cache-sg` | 6379 | `janmanch-app-sg` only |
| `janmanch-kafka-sg` | 9092 | `janmanch-app-sg` only |

The database, cache and Kafka are never reachable from the internet.

## 3. RDS (MySQL)

- Engine MySQL 8, free-tier template, `db.t3.micro` or `db.t4g.micro`.
- Database name `janmanch_db`, private subnets, security group `janmanch-db-sg`,
  public access **No**.
- Note the endpoint for `DB_URL`.

## 4. ElastiCache (Redis)

- Redis OSS, smallest node type, private subnets, `janmanch-cache-sg`.
- Leave encryption in transit **off** for this project; the application connects
  without TLS. Note the primary endpoint for `REDIS_HOST`.

## 5. S3

- Create a bucket (bucket names are global, so pick a unique one such as
  `janmanch-videos-<your-name>`). Keep "Block all public access" **on**.
- The application hands out links that expire after an hour.

## 6. Kafka on EC2

Amazon's managed Kafka (MSK) is not in the free tier, so run one broker in Docker:

- Launch a `t3.small` instance in a private subnet with `janmanch-kafka-sg`.
- Install Docker and run (replace `<private-ip>` with the instance's private IP):

```bash
docker run -d --name kafka --restart unless-stopped -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://<private-ip>:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  apache/kafka:3.8.0
```

## 7. Elastic Beanstalk

- Create an application `janmanch`, platform **Docker**, single instance.
- Upload a ZIP of the project (it contains the `Dockerfile`, which also installs FFmpeg).
- Network: the VPC above, public subnet, security group `janmanch-app-sg`.
- Instance profile: attach an IAM role that allows `s3:GetObject`, `s3:PutObject`
  and `s3:DeleteObject` on the bucket. No access keys are stored anywhere.
- Environment properties:

| Name | Value |
|---|---|
| `PORT` | `8080` |
| `DB_URL` | `jdbc:mysql://<rds-endpoint>:3306/janmanch_db` |
| `DB_USERNAME` / `DB_PASSWORD` | RDS credentials |
| `REDIS_HOST` | ElastiCache endpoint |
| `KAFKA_BOOTSTRAP_SERVERS` | `<kafka-private-ip>:9092` |
| `STORAGE_TYPE` | `s3` |
| `S3_BUCKET` | your bucket name |
| `AWS_REGION` | `ap-south-1` |
| `JWT_SECRET` | a random string of 32+ characters |

## 8. Check

Open the Beanstalk URL, sign up, upload a short video and watch it. Then confirm
the file appears in the S3 bucket and the row appears in RDS.

## 9. After the demo

Terminate the Beanstalk environment, the Kafka instance, ElastiCache and RDS, or
they keep costing money.

## Before a public launch

Add HTTPS (a domain with an ACM certificate on a load balancer), CloudFront in
front of S3 for faster streaming, database backups, and rate limiting.
