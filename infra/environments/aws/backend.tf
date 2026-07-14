# Backend S3 + DynamoDB — bucket/tabela criados por ../../bootstrap/bootstrap.sh.
#
# O sufixo -002754693932-us-east-1-an não é decoração: o bucket fica no "account regional
# namespace" da AWS, onde o nome é reservado a esta conta e não pode ser recriado por outra
# depois que o deletarmos. O backend só lê e escreve objetos — isso funciona igual nos dois
# namespaces, sem nenhum ajuste aqui.
#
# Bloco `backend` não aceita variável nem interpolação, então o nome é literal mesmo. Se for
# rodar em outra conta, troque o ID aqui (o bootstrap.sh deriva o nome sozinho e vai bater).
terraform {
  backend "s3" {
    bucket         = "oficina-api-tfstate-002754693932-us-east-1-an"
    key            = "aws/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "oficina-api-tfstate-lock"
    encrypt        = true
  }
}
