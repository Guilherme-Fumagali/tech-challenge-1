# Backend S3 + DynamoDB — bucket/tabela criados uma vez por ../../bootstrap.
# Nomes fixos (backend config não aceita variáveis); ajuste aqui se o bootstrap
# tiver sido rodado com bucket_name/lock_table_name diferentes dos defaults.
terraform {
  backend "s3" {
    bucket         = "oficina-api-tfstate"
    key            = "aws/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "oficina-api-tfstate-lock"
    encrypt        = true
  }
}
