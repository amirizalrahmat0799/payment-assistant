# Running the assistant on Amazon Bedrock

The default profile uses free local models (Ollama). The `aws` profile switches to Amazon Bedrock:
**Claude Haiku 4.5** for chat and **Titan Text Embeddings V2** for the knowledge base. This guide goes from a new
AWS account to a working assistant.

> **Cost:** Bedrock is pay-per-token. For a demo, a session of questions costs cents, but always set a budget
> alarm first (step 2) and check current prices on the [Bedrock pricing page](https://aws.amazon.com/bedrock/pricing/).

## 1. Create the account

Sign up at <https://aws.amazon.com/>. A valid payment method is required, including for AWS Marketplace
purchases, because third-party models such as Anthropic's are billed through the Marketplace.

Then secure the root user: enable MFA on it and don't use it day to day.

## 2. Set a budget alarm

**Billing and Cost Management → Budgets → Create budget → Use a template → Monthly cost budget**, e.g. USD 5,
with an email alert at 80%.

## 3. Create an IAM user for the app

**IAM → Users → Create user** (e.g. `pgs-assistant`), with no console access. Attach an inline policy that allows
only the two models this app uses (least privilege):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "InvokeAssistantModels",
      "Effect": "Allow",
      "Action": ["bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"],
      "Resource": [
        "arn:aws:bedrock:*::foundation-model/anthropic.claude-haiku-4-5-*",
        "arn:aws:bedrock:*:*:inference-profile/us.anthropic.claude-haiku-4-5-*",
        "arn:aws:bedrock:*::foundation-model/amazon.titan-embed-text-v2:0"
      ]
    },
    {
      "Sid": "FirstTimeModelSubscription",
      "Effect": "Allow",
      "Action": [
        "aws-marketplace:Subscribe",
        "aws-marketplace:Unsubscribe",
        "aws-marketplace:ViewSubscriptions"
      ],
      "Resource": "*"
    }
  ]
}
```

The Converse API is authorised by `bedrock:InvokeModel`. The Marketplace permissions are only needed the first time
a third-party model is used in the account; you can remove them afterwards.

Create an **access key** for this user (Security credentials → Create access key → *Application running outside AWS*).

## 4. Enable the Claude model

Serverless Bedrock models are enabled automatically the first time they are called. Anthropic additionally asks for a
one-time **use case form**: open the Bedrock console in **us-east-1**, go to **Model catalog → Claude Haiku 4.5**,
and submit it when prompted. A GitHub profile or project URL is accepted as the website.

## 5. Configure credentials locally

Install the [AWS CLI](https://aws.amazon.com/cli/), then:

```bash
aws configure --profile pgs-assistant     # paste the access key, region us-east-1, output json
```

Check that it works:

```bash
aws bedrock-runtime converse --profile pgs-assistant --region us-east-1 \
  --model-id us.anthropic.claude-haiku-4-5-20251001-v1:0 \
  --messages '[{"role":"user","content":[{"text":"Say hello"}]}]'
```

## 6. Run the assistant with the `aws` profile

From IntelliJ or the terminal:

```bash
SPRING_PROFILES_ACTIVE=aws AWS_PROFILE=pgs-assistant AWS_REGION=us-east-1 mvn spring-boot:run
```

Or with Docker Compose: uncomment the `SPRING_PROFILES_ACTIVE`, `AWS_*` and `volumes` lines for the `assistant`
service in `docker-compose.yml`.

The first start embeds the knowledge base with Titan (a few cents at most). The `aws` profile uses its own vector
table (`knowledge_titan`, 1024 dimensions), separate from the Ollama one, so you can switch back and forth.

## Options

| Setting | Default | Notes |
|---|---|---|
| `AWS_REGION` | `us-east-1` | The `us.` inference profile routes across US regions |
| `BEDROCK_CHAT_MODEL` | `us.anthropic.claude-haiku-4-5-20251001-v1:0` | e.g. `us.amazon.nova-lite-v1:0` for a cheaper Amazon model (update the IAM policy to match) |

## Running on AWS later

On AWS (ECS, EKS or EC2) don't use access keys: give the task or pod an **IAM role** with the same policy.
The app picks it up automatically through the default credentials chain.

## Troubleshooting

| Error | Cause |
|---|---|
| `AccessDeniedException ... not authorized to perform bedrock:InvokeModel` | IAM policy missing, or the model ID doesn't match its `Resource` |
| `AccessDeniedException` mentioning Marketplace or subscription | First use of the model needs the Marketplace permissions or a payment method; or the Anthropic form isn't submitted yet |
| `ValidationException ... on-demand throughput isn't supported` | Use the inference profile ID (`us.anthropic...`), not the plain model ID |
| `ThrottlingException` | New accounts have low quotas; wait and retry, or request an increase in Service Quotas |
