# Backend S3 + DynamoDB — bucket/tabela criados por ../../bootstrap/bootstrap.sh.
terraform {
  backend "s3" {
    bucket         = "oficina-api-tfstate-002754693932-us-east-1-an"
    key            = "aws/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "oficina-api-tfstate-lock"
    encrypt        = true
  }
}
